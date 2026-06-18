package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.label.entity.LabelContext;
import at.ac.tuwien.inso.ctrl.label.entity.PostLabelType;
import at.ac.tuwien.inso.ctrl.label.service.LabelingService;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabelingServiceUnitTest {

  @Mock private PostRepository postRepository;
  @Mock private FollowRelationRepository followRelationRepository;
  @Mock private FriendRelationRepository friendRelationRepository;

  @InjectMocks private LabelingService labelingService;

  /** buildContext */
  @Test
  void buildContext_shouldReturnContextWithFollowedAndSecondDegreeIds() {
    User user = buildUser();
    UUID followedId = UUID.randomUUID();
    UUID secondDegreeId = UUID.randomUUID();

    when(postRepository.findUpvotePercentile95()).thenReturn(42);
    when(friendRelationRepository.findFriendIds(user)).thenReturn(Set.of());
    when(followRelationRepository.findFollowedIds(user.getId())).thenReturn(Set.of(followedId));
    when(followRelationRepository.findSecondDegreeIds(user.getId()))
        .thenReturn(Set.of(secondDegreeId));

    LabelContext ctx = labelingService.buildContext(user);

    assertTrue(ctx.followedIds().contains(followedId));
    assertEquals(user.getId(), ctx.viewerId());
    assertTrue(ctx.secondDegreeIds().contains(secondDegreeId));
    assertEquals(42, ctx.upvoteThreshold());
    assertNotNull(ctx.now());
  }

  @Test
  void buildContext_whenPercentileIsNull_shouldDefaultThresholdToZero() {
    User user = buildUser();

    when(postRepository.findUpvotePercentile95()).thenReturn(null);
    when(followRelationRepository.findFollowedIds(user.getId())).thenReturn(Set.of());
    when(followRelationRepository.findSecondDegreeIds(user.getId())).thenReturn(Set.of());

    LabelContext ctx = labelingService.buildContext(user);

    assertEquals(0, ctx.upvoteThreshold());
  }

  @Test
  void buildContext_shouldSetNowToApproximatelyCurrentTime() {
    User user = buildUser();
    LocalDateTime before = LocalDateTime.now();

    when(postRepository.findUpvotePercentile95()).thenReturn(0);
    when(followRelationRepository.findFollowedIds(user.getId())).thenReturn(Set.of());
    when(followRelationRepository.findSecondDegreeIds(user.getId())).thenReturn(Set.of());

    LabelContext ctx = labelingService.buildContext(user);
    LocalDateTime after = LocalDateTime.now();

    assertFalse(ctx.now().isBefore(before));
    assertFalse(ctx.now().isAfter(after));
  }

  /** computeLabels - time-based labels */
  @ParameterizedTest
  @MethodSource("timeBasedLabelInputs")
  void computeLabels_shouldApplyCorrectTimeLabel(
      LocalDateTime createdAt, PostLabelType expectedLabel, PostLabelType absentLabel) {
    Post post = buildPost();
    post.setCreatedAt(createdAt);
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    if (expectedLabel != null) assertTrue(labels.contains(expectedLabel));
    if (absentLabel != null) assertFalse(labels.contains(absentLabel));
  }

  static Stream<Arguments> timeBasedLabelInputs() {
    LocalDateTime now = LocalDateTime.now();
    return Stream.of(
        Arguments.of(now.minusHours(1), PostLabelType.RECENT, PostLabelType.OLD_POST),
        Arguments.of(now.minusHours(5).minusMinutes(59), PostLabelType.RECENT, null),
        Arguments.of(now.minusMonths(7), PostLabelType.OLD_POST, PostLabelType.RECENT),
        Arguments.of(now.minusDays(10), null, PostLabelType.RECENT),
        Arguments.of(now.minusDays(10), null, PostLabelType.OLD_POST));
  }

  @Test
  void computeLabels_withNullCreatedAt_shouldNotAddTimeLabels() {
    Post post = buildPost();
    post.setCreatedAt(null);
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertFalse(labels.contains(PostLabelType.RECENT));
    assertFalse(labels.contains(PostLabelType.OLD_POST));
  }

  /** computeLabels - post property labels */
  @ParameterizedTest
  @MethodSource("postPropertyPresentLabelInputs")
  void computeLabels_shouldAddLabel_whenPostPropertyConditionIsMet(
      Boolean isAiGenerated,
      ExternalAuthor externalAuthor,
      Integer upvotes,
      int commentCount,
      PostLabelType expectedLabel) {
    Post post = buildPost();
    post.setIsAiGenerated(isAiGenerated);
    post.setExternalAuthor(externalAuthor);
    post.setUpvotes(upvotes);
    post.setComments(buildComments(commentCount));
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertTrue(labels.contains(expectedLabel));
  }

  static Stream<Arguments> postPropertyPresentLabelInputs() {
    ExternalAuthor author = mock(ExternalAuthor.class);
    return Stream.of(
        Arguments.of(true, null, 0, 0, PostLabelType.AI_GENERATED),
        Arguments.of(false, author, 0, 0, PostLabelType.MASTODON),
        Arguments.of(false, null, 5, 0, PostLabelType.POPULAR),
        Arguments.of(false, null, 100, 0, PostLabelType.POPULAR),
        Arguments.of(false, null, 0, 6, PostLabelType.HIGHLY_DISCUSSED));
  }

  @ParameterizedTest
  @MethodSource("postPropertyAbsentLabelInputs")
  void computeLabels_shouldNotAddLabel_whenPostPropertyConditionIsNotMet(
      Boolean isAiGenerated,
      ExternalAuthor externalAuthor,
      Integer upvotes,
      int commentCount,
      PostLabelType absentLabel) {
    Post post = buildPost();
    post.setIsAiGenerated(isAiGenerated);
    post.setExternalAuthor(externalAuthor);
    post.setUpvotes(upvotes);
    post.setComments(buildComments(commentCount));
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertFalse(labels.contains(absentLabel));
  }

  static Stream<Arguments> postPropertyAbsentLabelInputs() {
    return Stream.of(
        Arguments.of(false, null, 0, 0, PostLabelType.AI_GENERATED),
        Arguments.of(false, null, 0, 0, PostLabelType.MASTODON),
        Arguments.of(false, null, 4, 0, PostLabelType.POPULAR),
        Arguments.of(false, null, null, 0, PostLabelType.POPULAR),
        Arguments.of(false, null, 0, 5, PostLabelType.HIGHLY_DISCUSSED),
        Arguments.of(false, null, 0, 0, PostLabelType.HIGHLY_DISCUSSED));
  }

  /** computeLabels - source trust labels */
  @ParameterizedTest
  @MethodSource("sourceTrustLabelInputs")
  void computeLabels_shouldApplyCorrectSourceTrustLabel(
      int upvotes, int downvotes, Optional<PostLabelType> expectedLabel) {
    Post post = buildPost();
    post.setSource(buildSource(upvotes, downvotes));
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    expectedLabel.ifPresentOrElse(
        label -> assertTrue(labels.contains(label)),
        () -> {
          assertFalse(labels.contains(PostLabelType.TRUSTED_SOURCE));
          assertFalse(labels.contains(PostLabelType.DISPUTED_SOURCE));
        });
  }

  static Stream<Arguments> sourceTrustLabelInputs() {
    // Wilson score (z=1.96): score > 0.7 → TRUSTED_SOURCE, score < 0.3 → DISPUTED_SOURCE
    // (90, 10) → p=0.90, total=100 → score ≈ 0.832 → TRUSTED_SOURCE
    // (50, 0)  → p=1.00, total=50  → score ≈ 0.929 → TRUSTED_SOURCE
    // (1, 49)  → p=0.02, total=50  → score ≈ 0.003 → DISPUTED_SOURCE
    // (6, 6)   → p=0.50, total=12  → score ≈ 0.213 → DISPUTED_SOURCE
    // (5, 0)   → total=5 (≤5)      → no label applied (threshold not reached)
    return Stream.of(
        Arguments.of(90, 10, Optional.of(PostLabelType.TRUSTED_SOURCE)),
        Arguments.of(50, 0, Optional.of(PostLabelType.TRUSTED_SOURCE)),
        Arguments.of(1, 49, Optional.of(PostLabelType.DISPUTED_SOURCE)),
        Arguments.of(6, 6, Optional.of(PostLabelType.DISPUTED_SOURCE)),
        Arguments.of(5, 0, Optional.empty()));
  }

  @Test
  void computeLabels_withNullSource_shouldNotAddSourceLabels() {
    Post post = buildPost();
    post.setSource(null);
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertFalse(labels.contains(PostLabelType.TRUSTED_SOURCE));
    assertFalse(labels.contains(PostLabelType.DISPUTED_SOURCE));
  }

  @Test
  void computeLabels_withNullSourceVoteCounts_shouldNotAddSourceLabels() {
    Post post = buildPost();
    Source source = new Source();
    source.setUpvotes(null);
    source.setDownvotes(null);
    post.setSource(source);
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertFalse(labels.contains(PostLabelType.TRUSTED_SOURCE));
    assertFalse(labels.contains(PostLabelType.DISPUTED_SOURCE));
  }

  /** computeLabels - extended network label */
  @ParameterizedTest
  @MethodSource("extendedNetworkLabelInputs")
  void computeLabels_shouldApplyExtendedNetworkLabel_correctly(
      boolean inSecondDegree,
      boolean inDirectFollows,
      boolean inDirectFriends,
      boolean expectLabel) {
    UUID creatorId = UUID.randomUUID();
    User creator = buildUser();
    creator.setId(creatorId);

    Post post = buildPost();
    post.setUser(creator);

    Set<UUID> followedIds = inDirectFollows ? Set.of(creatorId) : Set.of();
    Set<UUID> friendIds   = inDirectFriends ? Set.of(creatorId) : Set.of();
    Set<UUID> secondDegreeIds = inSecondDegree ? Set.of(creatorId) : Set.of();
    UUID viewerId = UUID.randomUUID();
    LabelContext ctx =
        new LabelContext(viewerId, followedIds, friendIds, secondDegreeIds, 0, LocalDateTime.now());

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertEquals(expectLabel, labels.contains(PostLabelType.EXTENDED_NETWORK));
  }

  static Stream<Arguments> extendedNetworkLabelInputs() {
    return Stream.of(
        Arguments.of(true, false, false, true), // 2nd degree, no direct → Label
        Arguments.of(true, true, false, false), // 2nd degree but direct follow → no label
        Arguments.of(true, false, true, false), // 2nd degree but direct friend → no label
        Arguments.of(false, false, false, false), // nothing at all → no label
        Arguments.of(false, true, false, false), // direct follow only → no label
        Arguments.of(false, false, true, false) // only direct friend → no label
        );
  }

  @Test
  void computeLabels_withNullPostUser_shouldNotAddExtendedNetworkLabel() {
    Post post = buildPost();
    post.setUser(null);
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertFalse(labels.contains(PostLabelType.EXTENDED_NETWORK));
  }

  /** computeLabels - multiple labels */
  @Test
  void computeLabels_shouldReturnEmptySet_whenNoConditionsMet() {
    Post post = buildPost();
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertTrue(labels.isEmpty());
  }

  @Test
  void computeLabels_shouldReturnMultipleLabels_whenMultipleConditionsMet() {
    Post post = buildPost();
    post.setCreatedAt(LocalDateTime.now().minusHours(1));
    post.setIsAiGenerated(true);
    post.setUpvotes(10);
    LabelContext ctx = buildContext();

    Set<PostLabelType> labels = labelingService.computeLabels(post, ctx, false);

    assertTrue(labels.contains(PostLabelType.RECENT));
    assertTrue(labels.contains(PostLabelType.AI_GENERATED));
    assertTrue(labels.contains(PostLabelType.POPULAR));
  }

  /** Helpers */
  private User buildUser() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("testuser");
    return user;
  }

  private Post buildPost() {
    return Post.builder()
        .postId(UUID.randomUUID().toString())
        .isAiGenerated(false)
        .upvotes(0)
        .comments(new LinkedHashSet<>())
        .build();
  }

  private Source buildSource(int upvotes, int downvotes) {
    Source source = new Source();
    source.setUpvotes(upvotes);
    source.setDownvotes(downvotes);
    return source;
  }

  private LabelContext buildContext() {
    return new LabelContext(
        UUID.randomUUID(), Set.of(), Set.of(), Set.of(), 0, LocalDateTime.now());
  }

  private Set<Comment> buildComments(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> Comment.builder().commentId("comment-" + i).build())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}

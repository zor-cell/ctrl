package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.label.entity.PostLabelType;
import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostExplanation;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopic;
import at.ac.tuwien.inso.ctrl.post.entity.RecommendedPost;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedMapperUnitTest {

  @InjectMocks private FeedMapper feedMapper;

  /** toFeedPostResponse(RecommendedPost, postVotes, sourceVotes, labels) - author resolution */
  /*@Test
  void toFeedPostResponse_recommendedPost_withInternalUser_shouldResolveAuthorFieldsCorrectly() {
    Post post = buildInternalPost();
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), Set.of());

    assertEquals(post.getUser().getId().toString(), response.userId());
    assertEquals(post.getUser().getUsername(), response.username());
    assertEquals(post.getUser().getProfilePicture(), response.userProfilePic());
  }

  @Test
  void toFeedPostResponse_recommendedPost_withExternalAuthor_shouldResolveAuthorFieldsCorrectly() {
    Post post = buildExternalPost();
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), Set.of());

    assertNull(response.userId());
    assertEquals(post.getExternalAuthor().getDisplayName(), response.username());
    assertEquals(post.getExternalAuthor().getAvatarUrl(), response.userProfilePic());
  }

  /** toFeedPostResponse(RecommendedPost, postVotes, sourceVotes, labels) - core fields */
  /*@Test
  void toFeedPostResponse_recommendedPost_shouldMapCorePostFieldsCorrectly() {
    LocalDateTime now = LocalDateTime.now();
    Post post = buildInternalPost();
    post.setCaption("Test caption");
    post.setMediaRef("media.jpg");
    post.setCreatedAt(now);
    post.setUpvotes(10);
    post.setDownvotes(2);
    PostExplanation explanation = mock(PostExplanation.class);
    RecommendedPost recommendedPost = new RecommendedPost(post, explanation);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), Set.of());

    assertEquals(post.getPostId(), response.postId());
    assertEquals("Test caption", response.caption());
    assertEquals("media.jpg", response.mediaRef());
    assertEquals(now, response.createdAt());
    assertEquals(10, response.upvotes());
    assertEquals(2, response.downvotes());
    assertEquals(explanation, response.explanation());
  }

  /** toFeedPostResponse(RecommendedPost, postVotes, sourceVotes, labels) - vote mapping */
  /*@ParameterizedTest
  @MethodSource("postVoteInputs")
  void toFeedPostResponse_recommendedPost_shouldResolvePostVoteCorrectly(
      Map<String, String> postVotes, String expectedVote) {
    Post post = buildInternalPost();
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, postVotes, Map.of(), Set.of());

    assertEquals(expectedVote, response.userVote());
  }

  static Stream<Arguments> postVoteInputs() {
    return Stream.of(
        Arguments.of(Map.of("post-1", "UPVOTE"), "UPVOTE"),
        Arguments.of(Map.of("post-1", "DOWNVOTE"), "DOWNVOTE"),
        Arguments.of(Map.of(), null));
  }

  /** toFeedPostResponse(RecommendedPost, postVotes, sourceVotes, labels) - label mapping */
  /*@ParameterizedTest
  @MethodSource("labelMappingInputs")
  void toFeedPostResponse_recommendedPost_shouldMapLabelsCorrectly(
      Set<PostLabelType> inputLabels, int expectedCount) {
    Post post = buildInternalPost();
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), inputLabels);

    assertEquals(expectedCount, response.labelIds().size());
  }

  static Stream<Arguments> labelMappingInputs() {
    return Stream.of(
        Arguments.of(Set.of(), 0),
        Arguments.of(Set.of(PostLabelType.POPULAR), 1),
        Arguments.of(Set.of(PostLabelType.RECENT, PostLabelType.AI_GENERATED), 2));
  }

  @Test
  void toFeedPostResponse_recommendedPost_shouldMapLabelFieldsCorrectly() {
    Post post = buildInternalPost();
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(
            recommendedPost, Map.of(), Map.of(), Set.of(PostLabelType.POPULAR));

    LabelResponse label = response.labelIds().getFirst();
    assertEquals(PostLabelType.POPULAR.name(), label.labelId());
    assertEquals(PostLabelType.POPULAR.getName(), label.name());
    assertEquals(PostLabelType.POPULAR.getEmoji(), label.emoji());
    assertEquals(PostLabelType.POPULAR.getExplanation(), label.explanation());
  }

  /** toFeedPostResponse(RecommendedPost, postVotes, sourceVotes, labels) - source mapping */
  /*@Test
  void toFeedPostResponse_recommendedPost_withSource_shouldMapSourceFieldsCorrectly() {
    Post post = buildInternalPost();
    Source source = buildSource("src-1", 5, 2, List.of(buildSingleSource("https://example.com")));
    post.setSource(source);
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(
            recommendedPost, Map.of(), Map.of("src-1", "UPVOTE"), Set.of());

    assertEquals(1, response.sourceIds().size());
    FeedPostResponse.SourceResponse src = response.sourceIds().getFirst();
    assertEquals("src-1", src.sourceId());
    assertEquals("https://example.com", src.url());
    assertEquals(5, src.upvotes());
    assertEquals(2, src.downvotes());
    assertEquals("UPVOTE", src.userVote());
  }

  @Test
  void toFeedPostResponse_recommendedPost_withMultipleSingleSources_shouldMapAll() {
    Post post = buildInternalPost();
    Source source =
        buildSource(
            "src-1",
            5,
            2,
            List.of(buildSingleSource("https://a.com"), buildSingleSource("https://b.com")));
    post.setSource(source);
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), Set.of());

    assertEquals(2, response.sourceIds().size());
  }

  @Test
  void toFeedPostResponse_recommendedPost_withNoSource_shouldReturnEmptySourceList() {
    Post post = buildInternalPost();
    post.setSource(null);
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), Set.of());

    assertTrue(response.sourceIds().isEmpty());
  }

  /** toFeedPostResponse(RecommendedPost, postVotes, sourceVotes, labels) - comment mapping */
  /*@Test
  void toFeedPostResponse_recommendedPost_shouldMapCommentsCorrectly() {
    Post post = buildInternalPost();
    Comment comment = buildComment("c-1", null);
    post.setComments(new LinkedHashSet<>(Set.of(comment)));
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), Set.of());

    assertEquals(1, response.comments().size());
    FeedPostResponse.CommentResponse cr = response.comments().getFirst();
    assertEquals("c-1", cr.commentId());
    assertNull(cr.parentCommentId());
  }

  @Test
  void toFeedPostResponse_recommendedPost_withReplyComment_shouldIncludeParentCommentId() {
    Post post = buildInternalPost();
    Comment parent = buildComment("parent-1", null);
    Comment reply = buildComment("reply-1", parent);
    post.setComments(new LinkedHashSet<>(List.of(parent, reply)));
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), Set.of());

    FeedPostResponse.CommentResponse replyResponse =
        response.comments().stream()
            .filter(c -> c.commentId().equals("reply-1"))
            .findFirst()
            .orElseThrow();
    assertEquals("parent-1", replyResponse.parentCommentId());
  }

  /** toFeedPostResponse(RecommendedPost, postVotes, sourceVotes, labels) - topic mapping */
  /*@Test
  void toFeedPostResponse_recommendedPost_shouldMapTopicNamesCorrectly() {
    Post post = buildInternalPost();
    post.setPostTopics(new LinkedHashSet<>(Set.of(buildPostTopic("Technology"))));
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(recommendedPost, Map.of(), Map.of(), Set.of());

    assertEquals(List.of("Technology"), response.topicIds());
  }

  /** toFeedPostResponse(Post, postVotes, sourceVotes) - core behaviour */
  /*@Test
  void toFeedPostResponse_post_shouldMapCoreFieldsCorrectly() {
    LocalDateTime now = LocalDateTime.now();
    Post post = buildInternalPost();
    post.setCaption("Caption");
    post.setCreatedAt(now);
    post.setUpvotes(3);

    FeedPostResponse response = feedMapper.toFeedPostResponse(post, Map.of(), Map.of());

    assertEquals(post.getPostId(), response.postId());
    assertEquals("Caption", response.caption());
    assertEquals(now, response.createdAt());
    assertEquals(3, response.upvotes());
    assertNull(response.explanation());
  }

  @Test
  void toFeedPostResponse_post_shouldAlwaysReturnEmptyLabels() {
    Post post = buildInternalPost();

    FeedPostResponse response = feedMapper.toFeedPostResponse(post, Map.of(), Map.of());

    assertTrue(response.labelIds().isEmpty());
  }

  @Test
  void toFeedPostResponse_post_withInternalUser_shouldResolveAuthorFieldsCorrectly() {
    Post post = buildInternalPost();

    FeedPostResponse response = feedMapper.toFeedPostResponse(post, Map.of(), Map.of());

    assertEquals(post.getUser().getId().toString(), response.userId());
    assertEquals(post.getUser().getUsername(), response.username());
    assertEquals(post.getUser().getProfilePicture(), response.userProfilePic());
  }

  @Test
  void toFeedPostResponse_post_withExternalAuthor_shouldResolveAuthorFieldsCorrectly() {
    Post post = buildExternalPost();

    FeedPostResponse response = feedMapper.toFeedPostResponse(post, Map.of(), Map.of());

    assertNull(response.userId());
    assertEquals(post.getExternalAuthor().getDisplayName(), response.username());
    assertEquals(post.getExternalAuthor().getAvatarUrl(), response.userProfilePic());
  }

  @ParameterizedTest
  @MethodSource("postVoteInputs")
  void toFeedPostResponse_post_shouldResolvePostVoteCorrectly(
      Map<String, String> postVotes, String expectedVote) {
    Post post = buildInternalPost();

    FeedPostResponse response = feedMapper.toFeedPostResponse(post, postVotes, Map.of());

    assertEquals(expectedVote, response.userVote());
  }

  @Test
  void toFeedPostResponse_post_withSource_shouldMapSourceCorrectly() {
    Post post = buildInternalPost();
    Source source = buildSource("src-1", 4, 1, List.of(buildSingleSource("https://test.com")));
    post.setSource(source);

    FeedPostResponse response =
        feedMapper.toFeedPostResponse(post, Map.of(), Map.of("src-1", "DOWNVOTE"));

    assertEquals(1, response.sourceIds().size());
    assertEquals("DOWNVOTE", response.sourceIds().getFirst().userVote());
  }

  @Test
  void toFeedPostResponse_post_withNoSource_shouldReturnEmptySourceList() {
    Post post = buildInternalPost();
    post.setSource(null);

    FeedPostResponse response = feedMapper.toFeedPostResponse(post, Map.of(), Map.of());

    assertTrue(response.sourceIds().isEmpty());
  }

  /** toCommentResponse */
  @Test
  void toCommentResponse_withoutParent_shouldMapAllFieldsCorrectly() {
    Comment comment = buildComment("c-1", null);
    comment.setContent("Hello");
    comment.setCreatedAt(LocalDateTime.now());

    FeedPostResponse.CommentResponse response = feedMapper.toCommentResponse(comment);

    assertEquals("c-1", response.commentId());
    assertEquals(comment.getUser().getId().toString(), response.userId());
    assertEquals(comment.getUser().getUsername(), response.username());
    assertEquals("Hello", response.content());
    assertNull(response.parentCommentId());
  }

  @Test
  void toCommentResponse_withParent_shouldIncludeParentCommentId() {
    Comment parent = buildComment("parent-1", null);
    Comment reply = buildComment("reply-1", parent);

    FeedPostResponse.CommentResponse response = feedMapper.toCommentResponse(reply);

    assertEquals("parent-1", response.parentCommentId());
  }

  /** Helpers */
  private User buildInternalUser() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("alice");
    user.setProfilePicture("alice.jpg");
    return user;
  }

  private Post buildInternalPost() {
    Post post =
        Post.builder()
            .postId("post-1")
            .isAiGenerated(false)
            .upvotes(0)
            .downvotes(0)
            .comments(new LinkedHashSet<>())
            .postTopics(new LinkedHashSet<>())
            .build();
    post.setUser(buildInternalUser());
    return post;
  }

  private Post buildExternalPost() {
    ExternalAuthor externalAuthor = mock(ExternalAuthor.class);
    when(externalAuthor.getDisplayName()).thenReturn("External Author");
    when(externalAuthor.getAvatarUrl()).thenReturn("avatar.jpg");

    Post post =
        Post.builder()
            .postId("post-ext-1")
            .isAiGenerated(false)
            .upvotes(0)
            .downvotes(0)
            .comments(new LinkedHashSet<>())
            .postTopics(new LinkedHashSet<>())
            .build();
    post.setExternalAuthor(externalAuthor);
    return post;
  }

  private Source buildSource(
      String sourceId, int upvotes, int downvotes, List<SingleSource> singleSources) {
    Source source = new Source();
    source.setSourceId(sourceId);
    source.setUpvotes(upvotes);
    source.setDownvotes(downvotes);
    source.setSingleSources(singleSources);
    return source;
  }

  private SingleSource buildSingleSource(String url) {
    SingleSource ss = new SingleSource();
    ss.setUrl(url);
    return ss;
  }

  private Comment buildComment(String commentId, Comment parent) {
    Comment comment = new Comment();
    comment.setCommentId(commentId);
    comment.setUser(buildInternalUser());
    comment.setParentComment(parent);
    return comment;
  }

  private PostTopic buildPostTopic(String topicName) {
    Topic topic = Topic.builder().name(topicName).build();
    PostTopic postTopic = new PostTopic();
    postTopic.setTopic(topic);
    return postTopic;
  }
}

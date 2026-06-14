package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.algorithm.service.RecommendationService;
import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import at.ac.tuwien.inso.ctrl.label.entity.LabelContext;
import at.ac.tuwien.inso.ctrl.label.entity.PostLabelType;
import at.ac.tuwien.inso.ctrl.label.service.LabelingService;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.entity.RecommendedPost;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
public class FeedServiceUnitTest {

  @Mock private PostRepository postRepository;
  @Mock private PostVoteRepository postVoteRepository;
  @Mock private SourceVoteRepository sourceVoteRepository;
  @Mock private UserRepository userRepository;
  @Mock private FeedMapper feedMapper;
  @Mock private RecommendationService recommendationService;
  @Mock private LabelingService labelingService;
  @Mock private ReportService reportService;

  @InjectMocks private FeedService feedService;

  @Test
  void getFeed_withValidUsername_shouldFetchRecommendationsAndVotes() {
    String username = "alice";
    User user = buildUser(username);
    String userId = user.getId().toString();

    Post post = buildPost("post-1", "source-1");
    RecommendedPost recommendedPost = new RecommendedPost(post, null);

    PostVote postVote = buildPostVote("post-1", "UPVOTE");
    SourceVote sourceVote = buildSourceVote("source-1", "DOWNVOTE");
    FeedPostResponse expectedResponse = mock(FeedPostResponse.class);
    LabelContext labelCtx = mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(recommendationService.getRecommendedPosts(user, 0, 10, true))
        .thenReturn(List.of(recommendedPost));
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(userId, List.of("post-1")))
        .thenReturn(List.of(postVote));
    when(sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(userId, List.of("source-1")))
        .thenReturn(List.of(sourceVote));
    when(labelingService.buildContext(user)).thenReturn(labelCtx);
    when(labelingService.computeLabels(post, labelCtx)).thenReturn(labels);

    when(reportService.wasReportedByUser(user, ContentTypeEnum.POST, "post-1")).thenReturn(false);
    when(reportService.wasReportedByUser(user, ContentTypeEnum.SOURCE, "source-1"))
        .thenReturn(false);
    when(feedMapper.toFeedPostResponse(
            eq(recommendedPost),
            eq(Map.of("post-1", "UPVOTE")),
            eq(Map.of("post-1", false)),
            eq(Map.of("source-1", "DOWNVOTE")),
            eq(Map.of("source-1", false)),
            eq(labels)))
        .thenReturn(expectedResponse);

    List<FeedPostResponse> result = feedService.getFeed(0, 10, true, username);

    assertEquals(1, result.size());
    assertEquals(expectedResponse, result.getFirst());

    verify(postVoteRepository).findByIdUserIdAndIdPostIdIn(userId, List.of("post-1"));
    verify(sourceVoteRepository).findByIdUserIdAndIdSourceIdIn(userId, List.of("source-1"));
  }

  @Test
  void getFeed_withUnknownUsername_shouldThrowUsernameNotFoundException() {
    String username = "ghost";
    when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class, () -> feedService.getFeed(0, 10, true, username));
    verifyNoInteractions(
        recommendationService,
        postVoteRepository,
        sourceVoteRepository,
        feedMapper,
        labelingService);
  }

  @Test
  void getFeed_withNoRecommendations_shouldReturnEmptyList() {
    String username = "bob";
    User user = buildUser(username);
    String userId = user.getId().toString();
    LabelContext labelCtx = mock(LabelContext.class);

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(recommendationService.getRecommendedPosts(user, 0, 10, true)).thenReturn(List.of());
    when(labelingService.buildContext(user)).thenReturn(labelCtx);

    List<FeedPostResponse> result = feedService.getFeed(0, 10, true, username);

    assertTrue(result.isEmpty());
    verify(postVoteRepository).findByIdUserIdAndIdPostIdIn(userId, List.of());
    verify(sourceVoteRepository).findByIdUserIdAndIdSourceIdIn(userId, List.of());
    verifyNoInteractions(feedMapper);
  }

  @Test
  void getFeed_withPartialVotes_shouldMapCorrectly() {
    String username = "charlie";
    User user = buildUser(username);
    String userId = user.getId().toString();

    Post post1 = buildPost("post-1", null);
    RecommendedPost recPost1 = new RecommendedPost(post1, null);

    Post post2 = buildPost("post-2", "source-2");
    RecommendedPost recPost2 = new RecommendedPost(post2, null);

    PostVote mockedPostVote = buildPostVote("post-1", "DOWNVOTE");
    SourceVote mockedSourceVote = buildSourceVote("source-2", "UPVOTE");
    LabelContext labelCtx = mock(LabelContext.class);
    Set<PostLabelType> labels1 = Set.of();
    Set<PostLabelType> labels2 = Set.of();

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(recommendationService.getRecommendedPosts(user, 0, 10, true))
        .thenReturn(List.of(recPost1, recPost2));
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(userId, List.of("post-1", "post-2")))
        .thenReturn(List.of(mockedPostVote));
    when(sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(userId, List.of("source-2")))
        .thenReturn(List.of(mockedSourceVote));
    when(labelingService.buildContext(user)).thenReturn(labelCtx);
    when(labelingService.computeLabels(post1, labelCtx)).thenReturn(labels1);
    when(labelingService.computeLabels(post2, labelCtx)).thenReturn(labels2);
    when(reportService.wasReportedByUser(user, ContentTypeEnum.POST, "post-1")).thenReturn(false);
    when(reportService.wasReportedByUser(user, ContentTypeEnum.POST, "post-2")).thenReturn(false);
    when(reportService.wasReportedByUser(user, ContentTypeEnum.SOURCE, "source-2"))
        .thenReturn(false);

    feedService.getFeed(0, 10, true, username);

    verify(feedMapper)
        .toFeedPostResponse(
            eq(recPost1),
            eq(Map.of("post-1", "DOWNVOTE")),
            eq(Map.of("post-1", false, "post-2", false)),
            eq(Map.of("source-2", "UPVOTE")),
            eq(Map.of("source-2", false)),
            eq(labels1));
    verify(feedMapper)
        .toFeedPostResponse(
            eq(recPost2),
            eq(Map.of("post-1", "DOWNVOTE")),
            eq(Map.of("post-1", false, "post-2", false)),
            eq(Map.of("source-2", "UPVOTE")),
            eq(Map.of("source-2", false)),
            eq(labels2));
  }

  @Test
  void getFeed_withPrivateFeedFlag_shouldPassFlagToRecommendationService() {
    String username = "diana";
    User user = buildUser(username);
    LabelContext labelCtx = mock(LabelContext.class);

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(recommendationService.getRecommendedPosts(user, 0, 10, false)).thenReturn(List.of());
    when(labelingService.buildContext(user)).thenReturn(labelCtx);

    feedService.getFeed(0, 10, false, username);

    verify(recommendationService).getRecommendedPosts(user, 0, 10, false);
  }

  // --- Helpers ---
  private User buildUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }

  private Post buildPost(String postId, String sourceId) {
    Post post = new Post();
    post.setPostId(postId);
    if (sourceId != null) {
      Source source = new Source();
      source.setSourceId(sourceId);
      post.setSource(source);
    }
    return post;
  }

  private PostVote buildPostVote(String postId, String voteType) {
    PostVote vote = mock(PostVote.class);
    PostVoteId id = mock(PostVoteId.class);
    when(id.getPostId()).thenReturn(postId);
    when(vote.getId()).thenReturn(id);
    when(vote.getVoteType()).thenReturn(voteType);
    return vote;
  }

  private SourceVote buildSourceVote(String sourceId, String voteType) {
    SourceVote vote = mock(SourceVote.class);
    SourceVoteId id = mock(SourceVoteId.class);
    when(id.getSourceId()).thenReturn(sourceId);
    when(vote.getId()).thenReturn(id);
    when(vote.getVoteType()).thenReturn(voteType);
    return vote;
  }
}

package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.algorithm.service.RecommendationService;
import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.entity.RecommendedPost;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  @InjectMocks private FeedService feedService;

  @Test
  void getFeed_withValidUsername_shouldFetchRecommendationsAndVotes() {
    String username = "alice";
    User user = buildUser(username);
    String userId = user.getId().toString();

    Post post = buildPost("post-1", "source-1");
    RecommendedPost recommendedPost = new RecommendedPost();
    recommendedPost.setPost(post);

    PostVote postVote = buildPostVote("post-1", "UPVOTE");
    SourceVote sourceVote = buildSourceVote("source-1", "DOWNVOTE");
    FeedPostResponse expectedResponse = mock(FeedPostResponse.class);

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(recommendationService.getRecommendedPosts(user, 0, 10, true))
        .thenReturn(List.of(recommendedPost));
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(userId, List.of("post-1")))
        .thenReturn(List.of(postVote));
    when(sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(userId, List.of("source-1")))
        .thenReturn(List.of(sourceVote));
    when(feedMapper.toFeedPostResponse(
            eq(recommendedPost),
            eq(Map.of("post-1", "UPVOTE")),
            eq(Map.of("source-1", "DOWNVOTE"))))
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
        recommendationService, postVoteRepository, sourceVoteRepository, feedMapper);
  }

  @Test
  void getFeed_withNoRecommendations_shouldReturnEmptyList() {
    String username = "bob";
    User user = buildUser(username);
    String userId = user.getId().toString();

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(recommendationService.getRecommendedPosts(user, 0, 10, true)).thenReturn(List.of());

    List<FeedPostResponse> result = feedService.getFeed(0, 10, true, username);

    assertTrue(result.isEmpty());
    verify(postVoteRepository).findByIdUserIdAndIdPostIdIn(userId, List.of());
    verify(sourceVoteRepository).findByIdUserIdAndIdSourceIdIn(userId, List.of());
    verifyNoInteractions(feedMapper);
  }

  @Test
  void getFeed_withPartialVotes_shouldMapCorrectly() {
    // Arrange
    String username = "charlie";
    User user = buildUser(username);
    String userId = user.getId().toString();

    Post post1 = buildPost("post-1", null);
    RecommendedPost recPost1 = new RecommendedPost();
    recPost1.setPost(post1);

    Post post2 = buildPost("post-2", "source-2");
    RecommendedPost recPost2 = new RecommendedPost();
    recPost2.setPost(post2);

    PostVote mockedPostVote = buildPostVote("post-1", "DOWNVOTE");
    SourceVote mockedSourceVote = buildSourceVote("source-2", "UPVOTE");

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(recommendationService.getRecommendedPosts(user, 0, 10, true))
        .thenReturn(List.of(recPost1, recPost2));

    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(userId, List.of("post-1", "post-2")))
        .thenReturn(List.of(mockedPostVote));

    when(sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(userId, List.of("source-2")))
        .thenReturn(List.of(mockedSourceVote));

    feedService.getFeed(0, 10, true, username);

    verify(feedMapper)
        .toFeedPostResponse(
            eq(recPost1), eq(Map.of("post-1", "DOWNVOTE")), eq(Map.of("source-2", "UPVOTE")));

    verify(feedMapper)
        .toFeedPostResponse(
            eq(recPost2), eq(Map.of("post-1", "DOWNVOTE")), eq(Map.of("source-2", "UPVOTE")));
  }

  @Test
  void getFeed_withPrivateFeedFlag_shouldPassFlagToRecommendationService() {
    String username = "diana";
    User user = buildUser(username);

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(recommendationService.getRecommendedPosts(user, 0, 10, false)) // publicFeed = false
        .thenReturn(List.of());

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

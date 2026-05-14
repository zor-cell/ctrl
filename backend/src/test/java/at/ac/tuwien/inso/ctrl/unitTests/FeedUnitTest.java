package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.feed.service.impl.FeedServiceImpl;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class FeedUnitTest {

  @Mock private PostRepository postRepository;
  @Mock private PostVoteRepository postVoteRepository;
  @Mock private SourceVoteRepository sourceVoteRepository;
  @Mock private UserRepository userRepository;
  @Mock private FeedMapper feedMapper;

  @InjectMocks private FeedServiceImpl feedService;

  /** input validation * */
  @ParameterizedTest
  @MethodSource("invalidInputs")
  void getFeed_shouldThrow_whenInputIsInvalid(int page, int postsPerPage) {
    assertThrows(
        IllegalArgumentException.class, () -> feedService.getFeed(page, postsPerPage, true, null));
  }

  static Stream<Arguments> invalidInputs() {
    return Stream.of(
        Arguments.of(-1, 10), // page negative
        Arguments.of(0, 0), // postsPerPage is too small
        Arguments.of(0, -5), // postsPerPage negative
        Arguments.of(0, 101) // postsPerPage is too large
        );
  }

  @ParameterizedTest
  @MethodSource("validInputs")
  void getFeed_shouldNotThrow_whenInputIsValid(int page, int postsPerPage) {
    when(postRepository.findAllPaginated(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    when(postRepository.findAllWithAssociationsByIds(anyList())).thenReturn(List.of());

    assertDoesNotThrow(() -> feedService.getFeed(page, postsPerPage, true, null));
  }

  static Stream<Arguments> validInputs() {
    return Stream.of(
        Arguments.of(0, 1), // min postsPerPage
        Arguments.of(0, 100), // max postsPerPage
        Arguments.of(0, 10), // normal
        Arguments.of(5, 50) // higher page
        );
  }

  /** Anonymous user (username == null) * */
  @Test
  void getFeed_withNullUsername_shouldReturnEmptyVoteMaps() {
    // mocking postRepository
    Post post = buildPost("post-1");
    when(postRepository.findAllPaginated(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(post)));
    when(postRepository.findAllWithAssociationsByIds(List.of("post-1"))).thenReturn(List.of(post));

    // mocking feedMapper
    FeedPostResponse mockResponse = mock(FeedPostResponse.class);
    when(feedMapper.toFeedPostResponse(post, Map.of(), Map.of())).thenReturn(mockResponse);

    List<FeedPostResponse> result = feedService.getFeed(0, 10, true, null);

    assertEquals(1, result.size());
    // Ensure that repositories have NOT been queried for votes
    verifyNoInteractions(postVoteRepository, sourceVoteRepository, userRepository);
  }

  @Test
  void getFeed_withNullUsername_shouldReturnEmptyList_whenNoPostsExist() {
    // mocking postRepository
    when(postRepository.findAllPaginated(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    when(postRepository.findAllWithAssociationsByIds(List.of())).thenReturn(List.of());

    List<FeedPostResponse> result = feedService.getFeed(0, 10, true, null);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /** Logged-in user (username != null) * */
  @Test
  void getFeed_withValidUsername_shouldFetchVotesForUser() {
    String username = "alice";
    User user = buildUser(username);
    String userId = user.getId().toString();
    Post post = buildPost("post-1");

    // mocking userRepository, postRepository, sourceVoteRepository, feedMapper
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(postRepository.findAllPaginated(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(post)));
    when(postRepository.findAllWithAssociationsByIds(List.of("post-1"))).thenReturn(List.of(post));
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(userId, List.of("post-1")))
        .thenReturn(List.of());
    when(sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(eq(userId), anyList()))
        .thenReturn(List.of());
    when(feedMapper.toFeedPostResponse(any(Post.class), any(), any()))
        .thenReturn(mock(FeedPostResponse.class));

    List<FeedPostResponse> result = feedService.getFeed(0, 10,true,  username);

    assertEquals(1, result.size());
    // verify that method was called exactly once with exactly these arguments
    verify(postVoteRepository).findByIdUserIdAndIdPostIdIn(userId, List.of("post-1"));
  }

  @Test
  void getFeed_withValidUsername_shouldPassCorrectVotesToMapper() {
    String username = "bob";
    User user = buildUser(username);
    String userId = user.getId().toString();
    Post post = buildPost("post-42");

    // Simulate PostVote
    PostVote postVote = mock(PostVote.class);
    var postVoteId = mock(at.ac.tuwien.inso.ctrl.post.entity.PostVoteId.class);
    when(postVoteId.getPostId()).thenReturn("post-42");
    when(postVote.getId()).thenReturn(postVoteId);
    when(postVote.getVoteType()).thenReturn("UPVOTE");

    // mocking userRepository, postRepository, sourceVoteRepository, feedMapper
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(postRepository.findAllPaginated(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(post)));
    when(postRepository.findAllWithAssociationsByIds(List.of("post-42"))).thenReturn(List.of(post));
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(userId, List.of("post-42")))
        .thenReturn(List.of(postVote));
    when(sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(eq(userId), anyList()))
        .thenReturn(List.of());

    feedService.getFeed(0, 10,true,  username);

    // The mapper must be called with the correct vote map
    verify(feedMapper).toFeedPostResponse(post, Map.of("post-42", "UPVOTE"), Map.of());
  }

  @Test
  void getFeed_withUnknownUsername_shouldThrowUsernameNotFoundException() {
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class, () -> feedService.getFeed(0, 10, true, "ghost"));
  }

  /** Helpers * */
  private Post buildPost(String postId) {
    Post post = new Post();
    post.setPostId(postId);
    // source absichtlich null lassen → deckt den null-Check im Service ab
    return post;
  }

  private User buildUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }
}

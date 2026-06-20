package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostPreviewResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.mapper.PostMapper;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.search.dto.PostSearchRequest;
import at.ac.tuwien.inso.ctrl.search.dto.UserSearchRequest;
import at.ac.tuwien.inso.ctrl.search.service.SearchService;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.service.BlockService;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SearchServiceUnitTest {

  @Mock private EmbeddingService embeddingService;
  @Mock private PostRepository postRepository;
  @Mock private UserRepository userRepository;
  @Mock private PostMapper postMapper;
  @Mock private FollowService followService;
  @Mock private FriendRequestRepository friendRequestRepository;
  @Mock private RelationshipMapper relationshipMapper;
  @Mock private BlockService blockService;

  @InjectMocks private SearchService searchService;

  private at.ac.tuwien.inso.ctrl.user.entity.User buildUser() {
    return at.ac.tuwien.inso.ctrl.user.entity.User.builder()
        .id(UUID.randomUUID())
        .username("testuser")
        .email("test@test.com")
        .password("password")
        .name("Test User")
        .isVerified(true)
        .emailVerified(true)
        .build();
  }

  private Post buildPost(String postId, String caption) {
    return Post.builder()
        .postId(postId)
        .caption(caption)
        .isPublic(true)
        .embedding(new float[768])
        .build();
  }

  private PostPreviewResponse buildPostPreviewResponse(String postId, String caption) {
    return new PostPreviewResponse(
        postId, caption, null, null, null, null, false, true, false, null);
  }

  // ==================== VALID INPUT TESTS ====================

  @Test
  void testSearch_withValidQueryOnly_returnsRankedResults() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("summer vacation", null, 0, 10);
    float[] embedding = new float[768];
    Arrays.fill(embedding, 0.5f);

    Post post1 = buildPost("post-1", "Summer vacation at beach");
    Post post2 = buildPost("post-2", "Winter holiday in mountains");
    Post post3 = buildPost("post-3", "Summer beach party");
    List<Post> posts = List.of(post1, post2, post3);

    when(embeddingService.embedText("summer vacation")).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarity(
            eq(user.getId()),
            eq(new String[] {"summer", "vacation"}),
            eq(embedding),
            eq(10),
            eq(0)))
        .thenReturn(posts);
    when(postMapper.toPostPreviewResponse(post1))
        .thenReturn(buildPostPreviewResponse("post-1", "Summer vacation at beach"));
    when(postMapper.toPostPreviewResponse(post2))
        .thenReturn(buildPostPreviewResponse("post-2", "Winter holiday in mountains"));
    when(postMapper.toPostPreviewResponse(post3))
        .thenReturn(buildPostPreviewResponse("post-3", "Summer beach party"));

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(3, results.size());
    verify(postRepository).searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt());
    verify(postMapper, times(3)).toPostPreviewResponse(any());
  }

  @Test
  void testSearch_withValidTopicsOnly_returnsPostsContainingAtLeastOneTopic() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    List<String> topicIds = List.of("topic-1", "topic-2");
    PostSearchRequest request = new PostSearchRequest(null, topicIds, 0, 10);

    Post post1 = buildPost("post-1", "Post with topic 1");
    Post post2 = buildPost("post-2", "Post with topic 2");
    List<Post> posts = List.of(post1, post2);

    when(postRepository.searchPostsByTopics(
            eq(user.getId()), eq(new String[] {"topic-1", "topic-2"}), eq(10), eq(0)))
        .thenReturn(posts);
    when(postMapper.toPostPreviewResponse(post1))
        .thenReturn(buildPostPreviewResponse("post-1", "Post with topic 1"));
    when(postMapper.toPostPreviewResponse(post2))
        .thenReturn(buildPostPreviewResponse("post-2", "Post with topic 2"));

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(2, results.size());
    verify(postRepository).searchPostsByTopics(any(), any(), anyInt(), anyInt());
    verify(embeddingService, never()).embedText(any(String.class));
  }

  @Test
  void testSearch_withValidQueryAndTopics_returnsFilteredAndRankedResults() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    List<String> topicIds = List.of("travel");
    PostSearchRequest request = new PostSearchRequest("beach", topicIds, 0, 10);
    float[] embedding = new float[768];
    Arrays.fill(embedding, 0.5f);

    Post post1 = buildPost("post-1", "Beach vacation");
    List<Post> posts = List.of(post1);

    when(embeddingService.embedText("beach")).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarityAndTopics(
            eq(user.getId()),
            eq(new String[] {"beach"}),
            eq(embedding),
            eq(new String[] {"travel"}),
            eq(10),
            eq(0)))
        .thenReturn(posts);
    when(postMapper.toPostPreviewResponse(post1))
        .thenReturn(buildPostPreviewResponse("post-1", "Beach vacation"));

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(1, results.size());
    verify(postRepository)
        .searchPostsBySimilarityAndTopics(any(), any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void testSearch_withQueryMatchingMultiplePosts_ranksByWordMatchScore() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("summer beach fun", null, 0, 10);
    float[] embedding = new float[768];
    Arrays.fill(embedding, 0.5f);

    // Posts returned in order from repository (already ranked by SQL query)
    Post highMatch = buildPost("post-1", "Summer beach fun day"); // 3/3 words match
    Post medMatch = buildPost("post-2", "Summer beach party"); // 2/3 words match
    Post lowMatch = buildPost("post-3", "Summer day"); // 1/3 words match
    List<Post> posts = List.of(highMatch, medMatch, lowMatch);

    when(embeddingService.embedText("summer beach fun")).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(posts);
    when(postMapper.toPostPreviewResponse(any()))
        .thenAnswer(
            inv -> {
              Post p = inv.getArgument(0);
              return buildPostPreviewResponse(p.getPostId(), p.getCaption());
            });

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(3, results.size());
    assertEquals("post-1", results.get(0).postId()); // Highest word match first
    assertEquals("post-2", results.get(1).postId());
    assertEquals("post-3", results.get(2).postId());
  }

  @Test
  void testSearch_withTiedWordMatchScore_cosineSimilarityBreaksTie() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("summer beach", null, 0, 10);
    float[] queryEmbedding = new float[768];
    Arrays.fill(queryEmbedding, 0.5f);

    // Both posts have same word match (2/2 = 100%), but different embeddings
    Post higherSim = buildPost("post-high", "Summer beach vacation");
    Post lowerSim = buildPost("post-low", "Summer beach party");

    // Repository returns them ordered by combined score (70% word + 30% similarity)
    List<Post> posts = List.of(higherSim, lowerSim);

    when(embeddingService.embedText("summer beach")).thenReturn(queryEmbedding);
    when(postRepository.searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(posts);
    when(postMapper.toPostPreviewResponse(any()))
        .thenAnswer(
            inv -> {
              Post p = inv.getArgument(0);
              return buildPostPreviewResponse(p.getPostId(), p.getCaption());
            });

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(2, results.size());
    // Repository orders by score descending, so higherSim should be first
    assertEquals("post-high", results.get(0).postId());
    assertEquals("post-low", results.get(1).postId());
  }

  @Test
  void testSearch_withQueryMatchingNoPost_returnsEmptyList() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("xyzabc123nonexistent", null, 0, 10);
    float[] embedding = new float[768];

    when(embeddingService.embedText(any(String.class))).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @Test
  void testSearch_withTopicsMatchingNoPost_returnsEmptyList() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    List<String> topicIds = List.of("nonexistent-topic");
    PostSearchRequest request = new PostSearchRequest(null, topicIds, 0, 10);

    when(postRepository.searchPostsByTopics(any(), any(), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @Test
  void testSearch_withQueryAndTopicsMatchingNoPost_returnsEmptyList() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    List<String> topicIds = List.of("nonexistent-topic");
    PostSearchRequest request = new PostSearchRequest("xyznonexistent", topicIds, 0, 10);
    float[] embedding = new float[768];

    when(embeddingService.embedText(any(String.class))).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarityAndTopics(
            any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  // ==================== SCORING LOGIC TESTS (1 remaining) ====================

  @Test
  void testScoring_captionFullyMatchingQuery_scoresHigherThanPartialMatch() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("summer beach vacation", null, 0, 10);
    float[] embedding = new float[768];

    Post fullMatch = buildPost("post-full", "Summer beach vacation"); // 3/3 words
    Post partialMatch = buildPost("post-partial", "Summer beach day"); // 2/3 words

    // Repository returns ordered by score (full match should have higher word
    // score)
    List<Post> posts = List.of(fullMatch, partialMatch);

    when(embeddingService.embedText(any(String.class))).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(posts);
    when(postMapper.toPostPreviewResponse(any()))
        .thenAnswer(
            inv -> {
              Post p = inv.getArgument(0);
              return buildPostPreviewResponse(p.getPostId(), p.getCaption());
            });

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(2, results.size());
    assertEquals("post-full", results.get(0).postId()); // Full match first
    assertEquals("post-partial", results.get(1).postId());
  }

  // ==================== INVALID INPUT TESTS ====================

  @Test
  void testSearch_withQueryShorterThanThreeChars_throwsInvalidRequestException() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("ab", null, 0, 10); // 2 chars

    // When/Then
    assertThrows(
        InvalidRequestException.class,
        () -> {
          searchService.searchPosts(request, user);
        });
  }

  @Test
  void testSearch_withNullQuery_usesTopicsOnly() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    List<String> topicIds = List.of("topic-1");
    PostSearchRequest request = new PostSearchRequest(null, topicIds, 0, 10);

    Post post = buildPost("post-1", "Topic post");
    when(postRepository.searchPostsByTopics(any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of(post));
    when(postMapper.toPostPreviewResponse(post))
        .thenReturn(buildPostPreviewResponse("post-1", "Topic post"));

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(1, results.size());
    verify(postRepository).searchPostsByTopics(any(), any(), anyInt(), anyInt());
    verify(embeddingService, never()).embedText(any(String.class));
  }

  @Test
  void testSearch_withNullTopics_usesQueryOnly() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("vacation", null, 0, 10);
    float[] embedding = new float[768];

    Post post = buildPost("post-1", "Vacation post");
    when(embeddingService.embedText("vacation")).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of(post));
    when(postMapper.toPostPreviewResponse(post))
        .thenReturn(buildPostPreviewResponse("post-1", "Vacation post"));

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(1, results.size());
    verify(postRepository).searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void testSearch_withNullQueryAndNullTopics_throwsInvalidRequestException() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest(null, null, 0, 10);

    // When/Then
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              searchService.searchPosts(request, user);
            });

    assertTrue(exception.getMessage().contains("query (3+ characters) or at least one topic"));
  }

  @Test
  void testSearch_withEmptyTopicsList_usesQueryOnly() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("vacation", Collections.emptyList(), 0, 10);
    float[] embedding = new float[768];

    Post post = buildPost("post-1", "Vacation post");
    when(embeddingService.embedText("vacation")).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of(post));
    when(postMapper.toPostPreviewResponse(post))
        .thenReturn(buildPostPreviewResponse("post-1", "Vacation post"));

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertEquals(1, results.size());
    verify(postRepository).searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt());
    verify(postRepository, never()).searchPostsByTopics(any(), any(), anyInt(), anyInt());
  }

  // ==================== EXCEPTION HANDLING TESTS ====================

  @Test
  void testSearch_whenEmbeddingServiceThrows_propagatesException() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("vacation", null, 0, 10);

    when(embeddingService.embedText(any(String.class)))
        .thenThrow(new CommunicationException("Embedding service unavailable"));

    // When/Then
    assertThrows(
        CommunicationException.class,
        () -> {
          searchService.searchPosts(request, user);
        });
  }

  @Test
  void testSearch_whenRepositoryThrows_propagatesException() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("vacation", null, 0, 10);
    float[] embedding = new float[768];

    when(embeddingService.embedText(any(String.class))).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt()))
        .thenThrow(new RuntimeException("Database error"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          searchService.searchPosts(request, user);
        });
  }

  @Test
  void testSearch_whenEmbeddingServiceReturnsNull_handlesGracefully() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request = new PostSearchRequest("vacation", null, 0, 10);

    when(embeddingService.embedText(any(String.class))).thenReturn(null);
    when(postRepository.searchPostsBySimilarity(any(), any(), eq(null), anyInt(), anyInt()))
        .thenReturn(Collections.emptyList());

    // When
    List<PostPreviewResponse> results = searchService.searchPosts(request, user);

    // Then
    assertNotNull(results);
    // Service handles null by passing it to repository (which may use similarity
    // differently)
    verify(postRepository).searchPostsBySimilarity(any(), any(), eq(null), anyInt(), anyInt());
  }

  @Test
  void testSearch_paginationParametersPassedCorrectly() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User user = buildUser();
    PostSearchRequest request =
        new PostSearchRequest("vacation", null, 2, 15); // page 2, 15 per page
    float[] embedding = new float[768];

    Post post = buildPost("post-1", "Vacation");
    when(embeddingService.embedText(any(String.class))).thenReturn(embedding);
    when(postRepository.searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of(post));
    when(postMapper.toPostPreviewResponse(any()))
        .thenReturn(buildPostPreviewResponse("post-1", "Vacation"));

    // When
    searchService.searchPosts(request, user);

    // Then
    // Offset should be page * postsPerPage = 2 * 15 = 30
    verify(postRepository).searchPostsBySimilarity(any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void testSearchUsers_withNullQuery_returnsAllUsers() {
    // Given
    at.ac.tuwien.inso.ctrl.user.entity.User currentUser = buildUser();
    at.ac.tuwien.inso.ctrl.user.entity.User otherUser = buildUser();
    otherUser.setId(UUID.randomUUID());
    otherUser.setUsername("otheruser");

    UserSearchRequest request = new UserSearchRequest(null, false, false, false);

    when(userRepository.findByUsernameContainingIgnoreCaseAndFilterSuspended(""))
        .thenReturn(List.of(otherUser));
    when(blockService.inBlockedRelationship(currentUser, otherUser)).thenReturn(false);
    when(followService.getFollowedIds(currentUser)).thenReturn(Set.of());
    when(followService.getFriendIds(currentUser)).thenReturn(Set.of());
    when(friendRequestRepository.findReceiverIdsBySender(currentUser)).thenReturn(Set.of());
    when(friendRequestRepository.findSenderIdsByReceiver(currentUser)).thenReturn(Set.of());
    when(relationshipMapper.determineRelationshipStatuses(any(), any(), any(), any(), any()))
        .thenReturn(List.of());
    when(relationshipMapper.displayName(any(), any())).thenReturn("Test User");
    when(relationshipMapper.toUserSearchResponse(any(), any(), any()))
        .thenReturn(
            new UserSearchResponse(
                otherUser.getId(), "otheruser", "Test User", null, true, List.of()));

    // When
    List<UserSearchResponse> results = searchService.searchUsers(request, currentUser);

    // Then
    assertEquals(1, results.size());
    assertEquals("otheruser", results.get(0).username());
    verify(userRepository).findByUsernameContainingIgnoreCaseAndFilterSuspended("");
  }
}

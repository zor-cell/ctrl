package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.common.vote.VoteManager;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.mapper.PostMapper;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.post.service.impl.PostServiceImpl;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
public class PostServiceUnitTest {

  @Mock private PostRepository postRepository;

  @Mock private TopicRepository topicRepository;

  @Mock private UserRepository userRepository;

  @Mock private SourceService sourceService;

  @Mock private PostMapper postMapper;

  @Mock private EmbeddingService embeddingService;

  @Mock private PostVoteRepository postVoteRepository;

  @Mock private VoteManager voteManager;

  @Mock private FollowRelationRepository followRelationRepository;

  @Mock private FriendRelationRepository friendRelationRepository;

  @Mock private FileStorageService fileStorageService;

  @InjectMocks private PostServiceImpl postService;

  private User verifiedUser;
  private User unverifiedUser;
  private Source source;

  @BeforeEach
  void setUp() {
    verifiedUser = createUser("marlene", true);
    unverifiedUser = createUser("new-user", false);
    source = Source.builder().sourceId("source-1").build();
  }

  @Test
  void createPost_ShouldSavePostWithContent_WhenRequestIsValid() {
    CreatePostRequest request =
        new CreatePostRequest(
            "This is my post",
            "posts/media/image.png",
            true,
            true,
            List.of("topic-1", "topic-2"),
            null);
    Topic topicOne = Topic.builder().topicId("topic-1").name("Politics").build();
    Topic topicTwo = Topic.builder().topicId("topic-2").name("Science").build();
    float[] embedding = new float[] {0.1f, 0.2f};
    PostResponse response = createResponse("This is my post", true, true, "posts/media/image.png");

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(topicRepository.findAllById(List.of("topic-1", "topic-2")))
        .thenReturn(List.of(topicOne, topicTwo));
    when(embeddingService.embedText("This is my post")).thenReturn(embedding);
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    PostResponse result = postService.createPost(request, "marlene");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());
    Post savedPost = postCaptor.getValue();

    assertEquals(response, result);
    assertNotNull(savedPost.getPostId());
    assertEquals(verifiedUser, savedPost.getUser());
    assertEquals(source, savedPost.getSource());
    assertEquals("This is my post", savedPost.getCaption());
    assertEquals("posts/media/image.png", savedPost.getMediaRef());
    assertTrue(savedPost.isPublic());
    assertTrue(savedPost.isAiGenerated());
    assertEquals(0, savedPost.getUpvotes());
    assertEquals(0, savedPost.getDownvotes());
    assertNotNull(savedPost.getCreatedAt());
    assertArrayEquals(embedding, savedPost.getEmbedding());
    assertEquals(2, savedPost.getPostTopics().size());
    assertTrue(
        savedPost.getPostTopics().stream()
            .anyMatch(postTopic -> postTopic.getTopic().getTopicId().equals("topic-1")));
    assertTrue(
        savedPost.getPostTopics().stream()
            .anyMatch(postTopic -> postTopic.getTopic().getTopicId().equals("topic-2")));
    verify(sourceService).createSource(null);
    verify(embeddingService).embedText("This is my post");
    verify(fileStorageService).movePostMediaToPublicBucket("posts/media/image.png");
  }

  @Test
  void createPost_ShouldMakePostPrivate_WhenUserIsNotVerified() {
    CreatePostRequest request =
        new CreatePostRequest("Private until verified", null, true, false, List.of(), null);
    PostResponse response = createResponse("Private until verified", false, false, null);

    when(userRepository.findByUsername("new-user")).thenReturn(Optional.of(unverifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(embeddingService.embedText("Private until verified")).thenReturn(new float[] {1.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "new-user");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());

    assertFalse(postCaptor.getValue().isPublic());
    verify(fileStorageService, never()).movePostMediaToPublicBucket(any());
  }

  @Test
  void createPost_ShouldCreatePostWithoutTopics_WhenTopicIdsAreNull() {
    CreatePostRequest request =
        new CreatePostRequest("Post without topics", null, false, false, null, null);
    PostResponse response = createResponse("Post without topics", false, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(embeddingService.embedText("Post without topics")).thenReturn(new float[] {1.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "marlene");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());

    assertTrue(postCaptor.getValue().getPostTopics().isEmpty());
    verify(topicRepository, never()).findAllById(any());
  }

  @Test
  void createPost_ShouldCreatePostWithoutTopics_WhenTopicIdsAreEmpty() {
    CreatePostRequest request =
        new CreatePostRequest("Post without topics", null, false, false, List.of(), null);
    PostResponse response = createResponse("Post without topics", false, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(embeddingService.embedText("Post without topics")).thenReturn(new float[] {1.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "marlene");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());

    assertTrue(postCaptor.getValue().getPostTopics().isEmpty());
    verify(topicRepository, never()).findAllById(any());
  }

  @Test
  void createPost_ShouldUseDistinctTopicIds_WhenRequestContainsDuplicates() {
    CreatePostRequest request =
        new CreatePostRequest(
            "Post with duplicate topics", null, false, false, List.of("topic-1", "topic-1"), null);
    Topic topicOne = Topic.builder().topicId("topic-1").name("Politics").build();
    PostResponse response = createResponse("Post with duplicate topics", false, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(topicRepository.findAllById(List.of("topic-1"))).thenReturn(List.of(topicOne));
    when(embeddingService.embedText("Post with duplicate topics")).thenReturn(new float[] {1.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "marlene");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());

    assertEquals(1, postCaptor.getValue().getPostTopics().size());
    verify(topicRepository).findAllById(List.of("topic-1"));
  }

  @Test
  void createPost_ShouldMakePostPrivate_WhenVerifiedUserRequestsPrivatePost() {
    CreatePostRequest request =
        new CreatePostRequest("Private by choice", null, false, false, List.of(), null);
    PostResponse response = createResponse("Private by choice", false, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(embeddingService.embedText("Private by choice")).thenReturn(new float[] {1.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "marlene");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());

    assertFalse(postCaptor.getValue().isPublic());
  }

  @Test
  void createPost_ShouldThrowException_WhenTopicDoesNotExist() {
    CreatePostRequest request =
        new CreatePostRequest(
            "Missing topic", null, false, false, List.of("topic-1", "missing"), null);
    Topic topicOne = Topic.builder().topicId("topic-1").name("Politics").build();

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(topicRepository.findAllById(List.of("topic-1", "missing"))).thenReturn(List.of(topicOne));

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> postService.createPost(request, "marlene"));

    assertEquals("Topics not found: missing", exception.getMessage());
    verify(postRepository, never()).save(any());
    verify(embeddingService, never()).embedText(any(String.class));
  }

  @Test
  void createPost_ShouldThrowException_WhenUserDoesNotExist() {
    CreatePostRequest request =
        new CreatePostRequest("Unknown user", null, false, false, List.of(), null);

    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    assertThrows(UsernameNotFoundException.class, () -> postService.createPost(request, "unknown"));
    verify(sourceService, never()).createSource(any());
    verify(postRepository, never()).save(any());
  }

  @Test
  void getPostsForUser_withExistingPosts_returnsMappedPosts() {
    Post firstPost = Post.builder().postId("post-1").caption("First").build();
    Post secondPost = Post.builder().postId("post-2").caption("Second").build();
    PostResponse firstResponse = createResponse("First", false, false, null);
    PostResponse secondResponse = createResponse("Second", true, false, "posts/media/file.png");

    when(postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(firstPost, secondPost));
    when(postMapper.toResponse(firstPost)).thenReturn(firstResponse);
    when(postMapper.toResponse(secondPost)).thenReturn(secondResponse);

    List<PostResponse> result = postService.getPostsForUser("marlene");

    assertEquals(List.of(firstResponse, secondResponse), result);
    verify(postRepository).findAllByUserUsernameOrderByCreatedAtDesc("marlene");
    verify(postMapper).toResponse(firstPost);
    verify(postMapper).toResponse(secondPost);
  }

  @Test
  void getProfilePostsForUser_withExistingPosts_returnsMappedPublicPosts() {
    Post post = Post.builder().postId("post-1").caption("Profile post").build();
    PostResponse response = createResponse("Profile post", true, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(userRepository.findByUsername("new-user")).thenReturn(Optional.of(unverifiedUser));
    when(postRepository.findAllByUserUsernameAndIsPublicTrueOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(post));
    when(postMapper.toResponse(post)).thenReturn(response);

    List<PostResponse> result = postService.getProfilePostsForUser("marlene", "new-user");

    assertEquals(List.of(response), result);
    verify(postRepository).findAllByUserUsernameAndIsPublicTrueOrderByCreatedAtDesc("marlene");
    verify(postMapper).toResponse(post);
  }

  @Test
  void getProfilePostsForUser_withFriendRelation_returnsMappedAllPosts() {
    Post publicPost = Post.builder().postId("post-1").caption("Public").isPublic(true).build();
    Post privatePost = Post.builder().postId("post-2").caption("Private").isPublic(false).build();
    PostResponse publicResponse = createResponse("Public", true, false, null);
    PostResponse privateResponse = createResponse("Private", false, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(userRepository.findByUsername("new-user")).thenReturn(Optional.of(unverifiedUser));
    when(friendRelationRepository.existsByUsers(verifiedUser, unverifiedUser)).thenReturn(true);
    when(postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(publicPost, privatePost));
    when(postMapper.toResponse(publicPost)).thenReturn(publicResponse);
    when(postMapper.toResponse(privatePost)).thenReturn(privateResponse);

    List<PostResponse> result = postService.getProfilePostsForUser("marlene", "new-user");

    assertEquals(List.of(publicResponse, privateResponse), result);
    verify(postRepository).findAllByUserUsernameOrderByCreatedAtDesc("marlene");
    verify(postMapper).toResponse(publicPost);
    verify(postMapper).toResponse(privatePost);
  }

  @Test
  void deletePost_withOwnPost_deletesPost() {
    Post post = Post.builder().postId("post-1").user(verifiedUser).build();
    when(postRepository.findByPostIdAndUserUsername("post-1", "marlene"))
        .thenReturn(Optional.of(post));

    postService.deletePost("post-1", "marlene");

    verify(postRepository).delete(post);
    verify(fileStorageService, never()).deletePostMedia(any());
    verify(fileStorageService, never()).deletePublicPostMedia(any());
  }

  @Test
  void deletePost_withPublicMedia_deletesMediaFromPublicBucket() {
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .mediaRef("posts/media/file.png")
            .isPublic(true)
            .build();
    when(postRepository.findByPostIdAndUserUsername("post-1", "marlene"))
        .thenReturn(Optional.of(post));

    postService.deletePost("post-1", "marlene");

    verify(postRepository).delete(post);
    verify(fileStorageService).deletePublicPostMedia("posts/media/file.png");
    verify(fileStorageService, never()).deletePostMedia(any());
  }

  @Test
  void deletePost_withPrivateMedia_deletesMediaFromPrivateBucket() {
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .mediaRef("posts/media/file.png")
            .isPublic(false)
            .build();
    when(postRepository.findByPostIdAndUserUsername("post-1", "marlene"))
        .thenReturn(Optional.of(post));

    postService.deletePost("post-1", "marlene");

    verify(postRepository).delete(post);
    verify(fileStorageService).deletePostMedia("posts/media/file.png");
    verify(fileStorageService, never()).deletePublicPostMedia(any());
  }

  @Test
  void deletePost_withMissingOrForeignPost_throwsResourceNotFoundException() {
    when(postRepository.findByPostIdAndUserUsername("post-1", "marlene"))
        .thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> postService.deletePost("post-1", "marlene"));

    assertEquals("Post not found", exception.getMessage());
    verify(postRepository, never()).delete(any());
  }

  private User createUser(String username, boolean verified) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setVerified(verified);
    return user;
  }

  private PostResponse createResponse(
      String caption, boolean isPublic, boolean isAiGenerated, String mediaRef) {
    return new PostResponse(
        "post-1",
        verifiedUser.getId(),
        verifiedUser.getUsername(),
        source.getSourceId(),
        null,
        caption,
        mediaRef,
        mediaRef == null ? null : "http://localhost:3900/ctrl-dev-media/" + mediaRef,
        isPublic,
        isAiGenerated,
        List.of(),
        List.of(),
        List.of(),
        0,
        0,
        null);
  }
}

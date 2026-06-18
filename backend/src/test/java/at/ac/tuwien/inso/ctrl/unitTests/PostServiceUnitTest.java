package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.common.vote.VoteManager;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.exception.UnauthorizedException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.label.entity.LabelContext;
import at.ac.tuwien.inso.ctrl.label.entity.PostLabelType;
import at.ac.tuwien.inso.ctrl.label.service.LabelingService;
import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.DetailedPostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.mapper.PostMapper;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.post.service.impl.PostServiceImpl;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSingleSourceRequest;
import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.topic.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.user.dto.Role;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.BlockService;
import at.ac.tuwien.inso.ctrl.user.service.UserPreferenceService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  @Mock private SeenPostRepository seenPostRepository;
  @Mock private VoteManager voteManager;
  @Mock private FollowRelationRepository followRelationRepository;
  @Mock private FriendRelationRepository friendRelationRepository;
  @Mock private FileStorageService fileStorageService;
  @Mock private UserPreferenceService userPreferenceService;
  @Mock private LabelingService labelingService;
  @Mock private BlockService blockService;
  @Mock private ReportService reportService;

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
    String imageUrl = "http://garage:3900/ctrl-test-private/posts/media/image.png";
    PostResponse response = createResponse("This is my post", true, true, "posts/media/image.png");

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(topicRepository.findAllById(List.of("topic-1", "topic-2")))
        .thenReturn(List.of(topicOne, topicTwo));
    when(fileStorageService.resolveInternalPostMediaUrl("posts/media/image.png"))
        .thenReturn(imageUrl);
    when(embeddingService.embedTextWithImage("This is my post", imageUrl)).thenReturn(embedding);
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
    verify(fileStorageService).resolveInternalPostMediaUrl("posts/media/image.png");
    verify(embeddingService).embedTextWithImage("This is my post", imageUrl);
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
  void createPost_ShouldMakePostPrivate_WhenIsPublicIsNull() {
    CreatePostRequest request =
        new CreatePostRequest("Default private", null, null, false, List.of(), null);
    PostResponse response = createResponse("Default private", false, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(embeddingService.embedText("Default private")).thenReturn(new float[] {1.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "marlene");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());
    assertFalse(postCaptor.getValue().isPublic());
    verify(fileStorageService, never()).movePostMediaToPublicBucket(any());
  }

  @Test
  void createPost_ShouldDeduplicateTopicIdsBeforeLoadingTopics() {
    CreatePostRequest request =
        new CreatePostRequest(
            "Duplicate topics", null, false, false, List.of("topic-1", "topic-1"), null);
    Topic topic = Topic.builder().topicId("topic-1").name("Politics").build();
    PostResponse response = createResponse("Duplicate topics", false, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(topicRepository.findAllById(List.of("topic-1"))).thenReturn(List.of(topic));
    when(embeddingService.embedText("Duplicate topics")).thenReturn(new float[] {1.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "marlene");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());
    assertEquals(1, postCaptor.getValue().getPostTopics().size());
    verify(topicRepository).findAllById(List.of("topic-1"));
  }

  @Test
  void createPost_WithVideoMedia_UsesTextEmbeddingAndStillMovesPublicMedia() {
    CreatePostRequest request =
        new CreatePostRequest("Video post", "posts/media/video.mp4", true, false, List.of(), null);
    PostResponse response = createResponse("Video post", true, false, "posts/media/video.mp4");

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(null)).thenReturn(source);
    when(embeddingService.embedText("Video post")).thenReturn(new float[] {2.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "marlene");

    verify(embeddingService).embedText("Video post");
    verify(fileStorageService, never()).resolveInternalPostMediaUrl(any());
    verify(fileStorageService).movePostMediaToPublicBucket("posts/media/video.mp4");
  }

  @Test
  void createPost_WithSourceRequest_DelegatesSourceCreation() {
    CreateSourceRequest sourceRequest =
        new CreateSourceRequest(
            null,
            null,
            List.of(
                new CreateSingleSourceRequest(
                    "Journal", "Author", "Title", "https://x.test", null)));
    CreatePostRequest request =
        new CreatePostRequest("Post with source", null, false, false, List.of(), sourceRequest);
    PostResponse response = createResponse("Post with source", false, false, null);

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(sourceService.createSource(sourceRequest)).thenReturn(source);
    when(embeddingService.embedText("Post with source")).thenReturn(new float[] {1.0f});
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(postMapper.toResponse(any(Post.class))).thenReturn(response);

    postService.createPost(request, "marlene");

    ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
    verify(postRepository).save(postCaptor.capture());
    assertEquals(source, postCaptor.getValue().getSource());
    verify(sourceService).createSource(sourceRequest);
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
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(labelingService.buildContext(verifiedUser)).thenReturn(labelCtx);
    when(labelingService.computeLabels(firstPost, labelCtx, false)).thenReturn(labels);
    when(labelingService.computeLabels(secondPost, labelCtx, false)).thenReturn(labels);
    when(postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(firstPost, secondPost));
    when(postMapper.toResponse(firstPost, labels)).thenReturn(firstResponse);
    when(postMapper.toResponse(secondPost, labels)).thenReturn(secondResponse);

    List<PostResponse> result = postService.getPostsForUser("marlene");

    assertEquals(List.of(firstResponse, secondResponse), result);
    verify(postRepository).findAllByUserUsernameOrderByCreatedAtDesc("marlene");
    verify(postMapper).toResponse(firstPost, labels);
    verify(postMapper).toResponse(secondPost, labels);
  }

  @Test
  void getProfilePostsForUser_withExistingPosts_returnsMappedPublicPosts() {
    Post post = Post.builder().postId("post-1").caption("Profile post").build();
    PostResponse response = createResponse("Profile post", true, false, null);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(labelingService.buildContext(unverifiedUser)).thenReturn(labelCtx);
    when(labelingService.computeLabels(post, labelCtx, false)).thenReturn(labels);
    when(postRepository.findAllByUserUsernameAndIsPublicTrueOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(post));
    when(postMapper.toResponse(post, labels)).thenReturn(response);

    List<PostResponse> result = postService.getProfilePostsForUser("marlene", unverifiedUser);

    assertEquals(List.of(response), result);
    verify(friendRelationRepository).existsByUsers(verifiedUser, unverifiedUser);
    verify(postRepository).findAllByUserUsernameAndIsPublicTrueOrderByCreatedAtDesc("marlene");
    verify(postRepository, never()).findAllByUserUsernameOrderByCreatedAtDesc("marlene");
    verify(postMapper).toResponse(post, labels);
  }

  @Test
  void getProfilePostsForUser_withFriendRelation_returnsMappedAllPosts() {
    Post publicPost = Post.builder().postId("post-1").caption("Public").isPublic(true).build();
    Post privatePost = Post.builder().postId("post-2").caption("Private").isPublic(false).build();
    PostResponse publicResponse = createResponse("Public", true, false, null);
    PostResponse privateResponse = createResponse("Private", false, false, null);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(labelingService.buildContext(unverifiedUser)).thenReturn(labelCtx);
    when(labelingService.computeLabels(publicPost, labelCtx, false)).thenReturn(labels);
    when(labelingService.computeLabels(privatePost, labelCtx, false)).thenReturn(labels);
    when(friendRelationRepository.existsByUsers(verifiedUser, unverifiedUser)).thenReturn(true);
    when(postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(publicPost, privatePost));
    when(postMapper.toResponse(publicPost, labels)).thenReturn(publicResponse);
    when(postMapper.toResponse(privatePost, labels)).thenReturn(privateResponse);

    List<PostResponse> result = postService.getProfilePostsForUser("marlene", unverifiedUser);

    assertEquals(List.of(publicResponse, privateResponse), result);
    verify(postRepository).findAllByUserUsernameOrderByCreatedAtDesc("marlene");
    verify(postMapper).toResponse(publicPost, labels);
    verify(postMapper).toResponse(privatePost, labels);
  }

  @Test
  void getProfilePostsForUser_whenViewerIsProfileOwner_returnsMappedAllPosts() {
    Post privatePost = Post.builder().postId("post-1").caption("Private").isPublic(false).build();
    PostResponse response = createResponse("Private", false, false, null);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(labelingService.buildContext(verifiedUser)).thenReturn(labelCtx);
    when(labelingService.computeLabels(privatePost, labelCtx, false)).thenReturn(labels);
    when(postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(privatePost));
    when(postMapper.toResponse(privatePost, labels)).thenReturn(response);

    List<PostResponse> result = postService.getProfilePostsForUser("marlene", verifiedUser);

    assertEquals(List.of(response), result);
    verify(friendRelationRepository, never()).existsByUsers(any(), any());
    verify(postRepository).findAllByUserUsernameOrderByCreatedAtDesc("marlene");
  }

  @Test
  void getProfilePostsForUser_whenViewerIsAdmin_returnsMappedAllPosts() {
    User admin = createUser("admin", true);
    admin.setRole(Role.ADMIN);
    Post privatePost = Post.builder().postId("post-1").caption("Private").isPublic(false).build();
    PostResponse response = createResponse("Private", false, false, null);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(labelingService.buildContext(admin)).thenReturn(labelCtx);
    when(labelingService.computeLabels(privatePost, labelCtx, false)).thenReturn(labels);
    when(postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(privatePost));
    when(postMapper.toResponse(privatePost, labels)).thenReturn(response);

    List<PostResponse> result = postService.getProfilePostsForUser("marlene", admin);

    assertEquals(List.of(response), result);
    verify(postRepository).findAllByUserUsernameOrderByCreatedAtDesc("marlene");
    verify(postRepository, never()).findAllByUserUsernameAndIsPublicTrueOrderByCreatedAtDesc(any());
  }

  @Test
  void getPostsForUser_filtersCommentsFromBlockedUsersAndBlockedParents() {
    User commenter = createUser("commenter", true);
    User blockedUser = createUser("blocked", true);
    Comment visibleComment = comment("comment-1", commenter, null);
    Comment blockedComment = comment("comment-2", blockedUser, null);
    Comment replyToBlockedParent = comment("comment-3", commenter, blockedComment);
    Post post =
        Post.builder()
            .postId("post-1")
            .caption("Post")
            .comments(Set.of(visibleComment, blockedComment, replyToBlockedParent))
            .build();
    PostResponse response = createResponse("Post", false, false, null);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(labelingService.buildContext(verifiedUser)).thenReturn(labelCtx);
    when(labelingService.computeLabels(post, labelCtx, false)).thenReturn(labels);
    when(blockService.inBlockedRelationship(eq(verifiedUser), any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(1).equals(blockedUser));
    when(postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene"))
        .thenReturn(List.of(post));
    when(postMapper.toResponse(post, labels)).thenReturn(response);

    List<PostResponse> result = postService.getPostsForUser("marlene");

    assertEquals(List.of(response), result);
    assertEquals(Set.of(visibleComment), post.getComments());
  }

  @Test
  void getPostById_withPublicPost_mapsVoteAndReportState() {
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .source(source)
            .caption("Post")
            .isPublic(true)
            .comments(Set.of(comment("comment-1", unverifiedUser, null)))
            .build();
    PostVote vote =
        PostVote.builder()
            .id(new PostVoteId("post-1", verifiedUser.getId().toString()))
            .post(post)
            .voteType("up")
            .build();
    DetailedPostResponse response = org.mockito.Mockito.mock(DetailedPostResponse.class);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(
            verifiedUser.getId().toString(), List.of("post-1")))
        .thenReturn(List.of(vote));
    when(reportService.wasReportedByUser(verifiedUser, ContentTypeEnum.POST, "post-1"))
        .thenReturn(true);
    when(reportService.wasReportedByUser(verifiedUser, ContentTypeEnum.COMMENT, "comment-1"))
        .thenReturn(true);
    when(reportService.wasReportedByUser(verifiedUser, ContentTypeEnum.SOURCE, "source-1"))
        .thenReturn(true);
    when(labelingService.buildContext(verifiedUser)).thenReturn(labelCtx);
    when(labelingService.computeLabels(post, labelCtx, true)).thenReturn(labels);
    when(postMapper.toDetailedPostResponse(
            eq(post), eq(vote), eq(true), anyMap(), eq(true), eq(labels)))
        .thenReturn(response);

    DetailedPostResponse result = postService.getPostById("post-1", "marlene");

    assertEquals(response, result);
    verify(postMapper)
        .toDetailedPostResponse(
            eq(post),
            eq(vote),
            eq(true),
            eq(java.util.Map.of("comment-1", true)),
            eq(true),
            eq(labels));
  }

  @Test
  void getPostById_withPrivateOwnPostAndNoSource_mapsEmptyReportState() {
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .caption("Private")
            .isPublic(false)
            .comments(Set.of())
            .build();
    DetailedPostResponse response = org.mockito.Mockito.mock(DetailedPostResponse.class);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(
            verifiedUser.getId().toString(), List.of("post-1")))
        .thenReturn(List.of());
    when(labelingService.buildContext(verifiedUser)).thenReturn(labelCtx);
    when(labelingService.computeLabels(post, labelCtx, false)).thenReturn(labels);
    when(postMapper.toDetailedPostResponse(post, null, false, java.util.Map.of(), false, labels))
        .thenReturn(response);

    DetailedPostResponse result = postService.getPostById("post-1", "marlene");

    assertEquals(response, result);
    verify(reportService, never())
        .wasReportedByUser(eq(verifiedUser), eq(ContentTypeEnum.SOURCE), any());
  }

  @Test
  void getPostById_withPrivateFriendPost_returnsPost() {
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .caption("Private")
            .isPublic(false)
            .comments(Set.of())
            .build();
    DetailedPostResponse response = org.mockito.Mockito.mock(DetailedPostResponse.class);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
    when(userRepository.findByUsername("new-user")).thenReturn(Optional.of(unverifiedUser));
    when(friendRelationRepository.existsByUsers(unverifiedUser, verifiedUser)).thenReturn(true);
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(
            unverifiedUser.getId().toString(), List.of("post-1")))
        .thenReturn(List.of());
    when(labelingService.buildContext(unverifiedUser)).thenReturn(labelCtx);
    when(labelingService.computeLabels(post, labelCtx, false)).thenReturn(labels);
    when(postMapper.toDetailedPostResponse(post, null, false, java.util.Map.of(), false, labels))
        .thenReturn(response);

    DetailedPostResponse result = postService.getPostById("post-1", "new-user");

    assertEquals(response, result);
  }

  @Test
  void getPostById_withPrivatePostForNonFriend_throwsUnauthorized() {
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .caption("Private")
            .isPublic(false)
            .comments(Set.of())
            .build();

    when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
    when(userRepository.findByUsername("new-user")).thenReturn(Optional.of(unverifiedUser));

    assertThrows(UnauthorizedException.class, () -> postService.getPostById("post-1", "new-user"));
    verify(postMapper, never())
        .toDetailedPostResponse(any(), any(), anyBoolean(), anyMap(), anyBoolean(), any());
  }

  @Test
  void getPostById_withRemovedPost_throwsNotFound() {
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .caption("Removed")
            .isRemoved(true)
            .comments(Set.of())
            .build();

    when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
    when(userRepository.findByUsername("marlene")).thenReturn(Optional.of(verifiedUser));

    assertThrows(NotFoundException.class, () -> postService.getPostById("post-1", "marlene"));
    verify(postMapper, never())
        .toDetailedPostResponse(any(), any(), anyBoolean(), anyMap(), anyBoolean(), any());
  }

  @Test
  void getPostById_withPrivatePostForAdmin_returnsPost() {
    User admin = createUser("admin", true);
    admin.setRole(Role.ADMIN);
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .caption("Private")
            .isPublic(false)
            .comments(Set.of())
            .build();
    DetailedPostResponse response = org.mockito.Mockito.mock(DetailedPostResponse.class);
    LabelContext labelCtx = org.mockito.Mockito.mock(LabelContext.class);
    Set<PostLabelType> labels = Set.of();

    when(postRepository.findById("post-1")).thenReturn(Optional.of(post));
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(postVoteRepository.findByIdUserIdAndIdPostIdIn(
            admin.getId().toString(), List.of("post-1")))
        .thenReturn(List.of());
    when(labelingService.buildContext(admin)).thenReturn(labelCtx);
    when(labelingService.computeLabels(post, labelCtx, false)).thenReturn(labels);
    when(postMapper.toDetailedPostResponse(post, null, false, java.util.Map.of(), false, labels))
        .thenReturn(response);

    DetailedPostResponse result = postService.getPostById("post-1", "admin");

    assertEquals(response, result);
  }

  @Test
  void deletePost_withOwnPost_deletesPost() {
    Post post = Post.builder().postId("post-1").user(verifiedUser).build();
    when(postRepository.findByPostIdAndUserUsername("post-1", "marlene"))
        .thenReturn(Optional.of(post));

    postService.deletePost("post-1", "marlene");

    verify(seenPostRepository).deleteByPostId("post-1");
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
  void deletePost_withExternalMedia_doesNotDeleteMediaFromStorage() {
    Post post =
        Post.builder()
            .postId("post-1")
            .user(verifiedUser)
            .mediaRef("https://cdn.example.test/file.png")
            .isPublic(true)
            .build();
    when(postRepository.findByPostIdAndUserUsername("post-1", "marlene"))
        .thenReturn(Optional.of(post));

    postService.deletePost("post-1", "marlene");

    verify(seenPostRepository).deleteByPostId("post-1");
    verify(postRepository).delete(post);
    verify(fileStorageService, never()).deletePostMedia(any());
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
    user.setRole(Role.USER);
    return user;
  }

  private Comment comment(String commentId, User user, Comment parentComment) {
    return Comment.builder()
        .commentId(commentId)
        .user(user)
        .content("Comment")
        .createdAt(LocalDateTime.of(2026, 4, 25, 12, 0))
        .parentComment(parentComment)
        .build();
  }

  private PostResponse createResponse(
      String caption, boolean isPublic, boolean isAiGenerated, String mediaRef) {
    return new PostResponse(
        "post-1",
        verifiedUser.getId(),
        verifiedUser.getUsername(),
        false,
        source.getSourceId(),
        null,
        caption,
        mediaRef,
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

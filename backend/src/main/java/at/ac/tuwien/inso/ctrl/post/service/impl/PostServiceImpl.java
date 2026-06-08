package at.ac.tuwien.inso.ctrl.post.service.impl;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.common.vote.VoteManager;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.exception.UnauthorizedException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.label.entity.LabelContext;
import at.ac.tuwien.inso.ctrl.label.service.LabelingService;
import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.DetailedPostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopic;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopicId;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.mapper.PostMapper;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.post.service.PostService;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.UserPreferenceService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

  private static final int INITIAL_VOTE_COUNT = 0;

  private final PostRepository postRepository;
  private final TopicRepository topicRepository;
  private final UserRepository userRepository;
  private final SourceService sourceService;
  private final PostMapper postMapper;
  private final EmbeddingService embeddingService;
  private final PostVoteRepository postVoteRepository;
  private final SeenPostRepository seenPostRepository;
  private final VoteManager voteManager;
  private final FriendRelationRepository friendRelationRepository;
  private final FileStorageService fileStorageService;
  private final UserPreferenceService userPreferenceService;
  private final LabelingService labelingService;

  @Override
  @Transactional
  public PostResponse createPost(CreatePostRequest request, String username) {
    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    Source source = sourceService.createSource(request.source());

    var post =
        Post.builder()
            .postId(UUID.randomUUID().toString())
            .user(user)
            .source(source)
            .caption(request.caption())
            .mediaRef(request.mediaRef())
            .isPublic(resolveIsPublic(request, user.isVerified()))
            .isAiGenerated(Boolean.TRUE.equals(request.isAiGenerated()))
            .upvotes(INITIAL_VOTE_COUNT)
            .downvotes(INITIAL_VOTE_COUNT)
            .createdAt(LocalDateTime.now())
            .build();
    post.setPostTopics(buildPostTopics(post, request.topicIds()));

    float[] embedding = createEmbedding(post);
    post.setEmbedding(embedding);

    Post savedPost = postRepository.save(post);
    if (savedPost.isPublic()) {
      fileStorageService.movePostMediaToPublicBucket(savedPost.getMediaRef());
    }

    return postMapper.toResponse(savedPost);
  }

  @Override
  @Transactional
  public VoteResponse votePost(String postId, VoteRequest request, String username) {
    Post post = fetchPost(postId);
    var user = fetchUser(username);

    String newVoteType = request.voteType();
    PostVoteId voteId = new PostVoteId(postId, user.getId().toString());
    Optional<PostVote> existingVote = postVoteRepository.findById(voteId);

    VoteResponse response =
        voteManager.processVote(
            post,
            existingVote,
            newVoteType,
            /* createVote */ () ->
                postVoteRepository.save(
                    PostVote.builder().id(voteId).post(post).voteType(newVoteType).build()),
            /* deleteVote */ () -> postVoteRepository.delete(existingVote.get()),
            /* updateVote */ () -> {
              existingVote.get().setVoteType(newVoteType);
              postVoteRepository.save(existingVote.get());
            },
            /* saveEntity */ () -> postRepository.save(post));

    // mark post as seen if not already recorded
    seenPostRepository.upsertView(user.getId(), postId, 0L);

    // update user preference asynchronously
    userPreferenceService.updateInteractionVectorAsync(user.getId());
    return response;
  }

  /** Helpers * */
  private float[] createEmbedding(Post post) {
    String mediaRef = post.getMediaRef();
    if (isEmbeddableImageReference(mediaRef)) {
      String imageUrl = fileStorageService.resolveInternalPostMediaUrl(mediaRef);
      return embeddingService.embedTextWithImage(post.getCaption(), imageUrl);
    }

    return embeddingService.embedText(post.getCaption());
  }

  private boolean isEmbeddableImageReference(String mediaRef) {
    if (mediaRef == null || mediaRef.isBlank()) {
      return false;
    }

    String normalizedRef = mediaRef.toLowerCase(Locale.ROOT);
    int queryStart = normalizedRef.indexOf('?');
    if (queryStart >= 0) {
      normalizedRef = normalizedRef.substring(0, queryStart);
    }

    int fragmentStart = normalizedRef.indexOf('#');
    if (fragmentStart >= 0) {
      normalizedRef = normalizedRef.substring(0, fragmentStart);
    }

    return normalizedRef.endsWith(".jpg")
        || normalizedRef.endsWith(".jpeg")
        || normalizedRef.endsWith(".png")
        || normalizedRef.endsWith(".webp")
        || normalizedRef.endsWith(".gif");
  }

  @Transactional(readOnly = true)
  public List<PostResponse> getPostsForUser(String username) {
    User user = fetchUser(username);
    LabelContext ctx = labelingService.buildContext(user);
    return postRepository.findAllByUserUsernameOrderByCreatedAtDesc(username).stream()
        .map(post -> postMapper.toResponse(post, labelingService.computeLabels(post, ctx)))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PostResponse> getProfilePostsForUser(String profileUsername, User viewer) {
    User profileOwner = fetchUser(profileUsername);
    LabelContext ctx = labelingService.buildContext(viewer);

    boolean canSeePrivatePosts =
        profileOwner.getId().equals(viewer.getId())
            || friendRelationRepository.existsByUsers(profileOwner, viewer);

    List<Post> posts =
        canSeePrivatePosts
            ? postRepository.findAllByUserUsernameOrderByCreatedAtDesc(profileUsername)
            : postRepository.findAllByUserUsernameAndIsPublicTrueOrderByCreatedAtDesc(
                profileUsername);

    return posts.stream()
        .map(post -> postMapper.toResponse(post, labelingService.computeLabels(post, ctx)))
        .toList();
  }

  @Override
  @Transactional
  public void deletePost(String postId, String username) {
    Post post =
        postRepository
            .findByPostIdAndUserUsername(postId, username)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

    postRepository.delete(post);
    deleteLocalPostMedia(post);
  }

  private boolean resolveIsPublic(CreatePostRequest request, boolean isVerified) {
    return isVerified && Boolean.TRUE.equals(request.isPublic());
  }

  private void deleteLocalPostMedia(Post post) {
    String mediaRef = post.getMediaRef();
    if (mediaRef == null || mediaRef.startsWith("http://") || mediaRef.startsWith("https://")) {
      return;
    }

    if (post.isPublic()) {
      fileStorageService.deletePublicPostMedia(mediaRef);
    } else {
      fileStorageService.deletePostMedia(mediaRef);
    }
  }

  private Set<PostTopic> buildPostTopics(Post post, List<String> topicIds) {
    if (topicIds == null || topicIds.isEmpty()) {
      return new LinkedHashSet<>();
    }

    List<String> uniqueTopicIds = topicIds.stream().distinct().toList();
    List<Topic> topics = topicRepository.findAllById(uniqueTopicIds);

    if (topics.size() != uniqueTopicIds.size()) {
      Set<String> foundTopicIds =
          topics.stream().map(Topic::getTopicId).collect(Collectors.toSet());
      String missingTopicIds =
          uniqueTopicIds.stream()
              .filter(topicId -> !foundTopicIds.contains(topicId))
              .sorted()
              .collect(Collectors.joining(", "));
      throw new ResourceNotFoundException("Topics not found: " + missingTopicIds);
    }

    return topics.stream()
        .map(topic -> createPostTopic(post, topic))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private PostTopic createPostTopic(Post post, Topic topic) {
    return PostTopic.builder()
        .id(new PostTopicId(post.getPostId(), topic.getTopicId()))
        .post(post)
        .topic(topic)
        .build();
  }

  private Post fetchPost(String postId) {
    return postRepository
        .findById(postId)
        .orElseThrow(() -> new EntityNotFoundException("Post not found: " + postId));
  }

  private User fetchUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }

  @Override
  public DetailedPostResponse getPostById(String postId, String username) {
    Post post = this.fetchPost(postId);
    User user = this.fetchUser(username);

    if (Boolean.TRUE.equals(post.getIsRemoved())) {
      throw new NotFoundException("Post", postId);
    }

    if (!post.isPublic()) {
      if (post.getUser() != null
          && !post.getUser().getUsername().equals(username)
          && !friendRelationRepository.existsByUsers(user, post.getUser())) {
        throw new UnauthorizedException("post with Id " + postId);
      }
    }

    List<PostVote> userVote =
        this.postVoteRepository.findByIdUserIdAndIdPostIdIn(
            user.getId().toString(), List.of(postId.toString()));

    PostVote toMap = userVote.isEmpty() ? null : userVote.getFirst();

    return this.postMapper.toDetailedPostResponse(post, toMap);
  }
}

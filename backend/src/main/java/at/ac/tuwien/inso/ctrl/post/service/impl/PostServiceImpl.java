package at.ac.tuwien.inso.ctrl.post.service.impl;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.common.vote.VoteManager;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
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
import at.ac.tuwien.inso.ctrl.post.service.PostService;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
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
  private final VoteManager voteManager;

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

    // embed post information (only text for now, image when file storage service is up)
    float[] embedding = embeddingService.embedText(post.getCaption());
    post.setEmbedding(embedding);

    return postMapper.toResponse(postRepository.save(post));
  }

  @Override
  @Transactional
  public VoteResponse votePost(String postId, VoteRequest request, String username) {
    Post post = fetchPost(postId);
    var user = fetchUser(username);

    String newVoteType = request.voteType();
    PostVoteId voteId = new PostVoteId(postId, user.getId().toString());
    Optional<PostVote> existingVote = postVoteRepository.findById(voteId);

    return voteManager.processVote(
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
  }

  /** Helpers * */
  @Transactional(readOnly = true)
  public List<PostResponse> getPostsForUser(String username) {
    return postRepository.findAllByUserUsernameOrderByCreatedAtDesc(username).stream()
        .map(postMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PostResponse> getProfilePostsForUser(String username) {
    return postRepository.findAllByUserUsernameOrderByCreatedAtDesc(username).stream()
        .map(postMapper::toResponse)
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
  }

  private boolean resolveIsPublic(CreatePostRequest request, boolean isVerified) {
    return isVerified && Boolean.TRUE.equals(request.isPublic());
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
}

package at.ac.tuwien.inso.ctrl.post.service.impl;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopic;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopicId;
import at.ac.tuwien.inso.ctrl.post.mapper.PostMapper;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.service.PostService;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
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

    //embed post information (only text for now, image when file storage service is up)
    float[] embedding = embeddingService.embedText(post.getCaption());
    post.setEmbedding(embedding);

    return postMapper.toResponse(postRepository.save(post));
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
}

package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostExplanation;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostScoreProjection;
import at.ac.tuwien.inso.ctrl.post.entity.RecommendedPost;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {
  private final AlgorithmService algorithmService;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final TopicRepository topicRepository;

  @Transactional
  public List<RecommendedPost> getRecommendedPosts(User user, int page, int size) {
    var config = algorithmService.getConfig(user);

    List<Float> interactionVectorList =
        userRepository.calculateUserInteractionPreferenceVector(user.getId());
    float[] interactionPreference = vectorFromVectorList(interactionVectorList);

    List<Float> topicVectorList = topicRepository.calculateTopicPreferenceVector(config.topics());
    float[] topicPreference = vectorFromVectorList(topicVectorList);

    List<String> candidatePosts = postRepository.findCandidatePool(user.getId(), true, interactionPreference, topicPreference);

    List<PostScoreProjection> scoredProjections =
        postRepository.findPersonalizedFeed(
            candidatePosts,
            user.getId(),
            config.weightInteraction() / 100.0,
            interactionPreference,
            config.weightTopic() / 100.0,
            topicPreference,
            config.topics(),
            config.weightRecency() / 100.0,
            config.weightTrending() / 100.0,
            config.weightSourceQuality() / 100.0,
            config.weightExploration() / 100.0,
            size,
            page * size);
    if (scoredProjections.isEmpty()) {
      return List.of();
    }

    // fetch all posts from the db and create lookup table for retrieval
    List<String> postIds = scoredProjections.stream().map(PostScoreProjection::getPostId).toList();
    List<Post> fullPosts = postRepository.findAllById(postIds);
    Map<String, Post> postMap =
        fullPosts.stream().collect(Collectors.toMap(Post::getPostId, post -> post));

    return scoredProjections.stream()
        .map(
            score -> {
              Post post = postMap.get(score.getPostId());
              PostExplanation explanation =
                  new PostExplanation(
                      score.getScoreInteraction(),
                      score.getScoreTopic(),
                      score.getScoreRecency(),
                      score.getScoreTrending(),
                      score.getScoreSource(),
                      score.getScoreExploration(),
                      score.getTotalScore());

              return new RecommendedPost(post, explanation);
            })
        .toList();
  }

  private float[] vectorFromVectorList(List<Float> vectorList) {
    if (vectorList == null || vectorList.isEmpty()) {
      return null;
    }

    boolean hasData = false;
    float[] vector = new float[vectorList.size()];
    for (int i = 0; i < vectorList.size(); i++) {
      float val = vectorList.get(i) != null ? vectorList.get(i) : 0f;
      vector[i] = val;
      if (val != 0f) {
        hasData = true;
      }
    }

    return hasData ? vector : null;
  }
}

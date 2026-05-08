package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.AlgorithmConfigResponse;
import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.embedding.config.EmbeddingModelConfig;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.Optional;

import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {
  private final PostRepository postRepository;
  private final AlgorithmService algorithmService;
  private final AlgorithmRepository algorithmRepository;
  private final UserRepository userRepository;
  private final TopicRepository topicRepository;

  public List<Post> getRecommendedPosts(User user, int page, int size) {
    Optional<AlgorithmConfig> configOpt = algorithmRepository.findByUserId(user.getId());
    if(configOpt.isEmpty()) {
      throw new ResourceNotFoundException("Algorithm configuration not found");
    }
    AlgorithmConfig config = configOpt.get();

    List<Float> interactionVectorList =
        userRepository.calculateUserInteractionPreferenceVector(user.getId());
    float[] interactionPreference = vectorFromVectorList(interactionVectorList);

    List<String> topicIds = config.getTopics().stream().map(Topic::getTopicId).toList();
    List<Float> topicVectorList = topicRepository.calculateTopicPreferenceVector(topicIds);
    float[] topicPreference = vectorFromVectorList(topicVectorList);

    int offset = page * size;

    return postRepository.findPersonalizedFeed(
        user.getId(),
        config.getWeightInteraction() / 100.0,
        interactionPreference,
        config.getWeightTopic() / 100.0,
        topicPreference,
        topicIds,
        config.getWeightRecency() / 100.0,
        config.getWeightTrending() / 100.0,
        config.getWeightSourceQuality() / 100.0,
        config.getWeightExploration() / 100.0,
        size,
        offset);
  }

  private float[] vectorFromVectorList(List<Float> vectorList) {
    float[] vector;
    if (vectorList == null || vectorList.isEmpty()) {
      vector = new float[EmbeddingModelConfig.MODEL_DIMENSIONS];
    } else {
      vector = new float[vectorList.size()];
      for (int i = 0; i < vectorList.size(); i++) {
        vector[i] = vectorList.get(i) != null ? vectorList.get(i) : 0f;
      }
    }
    return vector;
  }
}

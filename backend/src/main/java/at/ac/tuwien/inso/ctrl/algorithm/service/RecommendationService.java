package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.AlgorithmConfigResponse;
import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.embedding.config.EmbeddingModelConfig;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;

import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {
  private final PostRepository postRepository;
  private final AlgorithmService algorithmService;
  private final UserRepository userRepository;
  private final TopicRepository topicRepository;

  public List<Post> getRecommendedPosts(User user, int page, int size) {
    AlgorithmConfigResponse config = algorithmService.getConfig(user);

    List<Float> interactionVectorList =
        userRepository.calculateUserInteractionPreferenceVector(user.getId().toString());
    float[] userInteractionPreference = vectorFromVectorList(interactionVectorList);

    List<String> topicIds = config.topics().stream().map(TopicResponse::topicId).toList();
    List<Float> topicVectorList = topicRepository.calculateTopicPreferenceVector(topicIds);
    float[] topicPreference = vectorFromVectorList(topicVectorList);

    int offset = page * size;

    return postRepository.findPersonalizedFeed(
        user.getId().toString(),
        userInteractionPreference,
        config.weightTopic() / 100.0,
        config.weightRecency() / 100.0,
        config.weightTrending() / 100.0,
        config.weightSourceQuality() / 100.0,
        config.weightExploration() / 100.0,
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

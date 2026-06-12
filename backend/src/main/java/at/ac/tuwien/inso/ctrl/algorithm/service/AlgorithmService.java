package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.AlgorithmConfigDto;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.mapper.AlgorithmMapper;
import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.embedding.VectorUtils;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.topic.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlgorithmService {
  private final AlgorithmRepository algorithmRepository;
  private final AlgorithmMapper algorithmMapper;
  private final TopicRepository topicRepository;

  @Transactional
  public AlgorithmConfigDto getConfig(User currentUser) {
    AlgorithmConfig config = getOrCreateConfig(currentUser);

    return algorithmMapper.toConfig(config);
  }

  @Transactional
  public AlgorithmConfigDto updateConfig(User currentUser, AlgorithmConfigDto algorithmConfig) {
    AlgorithmConfig config = getOrCreateConfig(currentUser);

    config.setWeightInteraction(algorithmConfig.weightInteraction());
    config.setWeightRecency(algorithmConfig.weightRecency());
    config.setWeightExploration(algorithmConfig.weightExploration());
    config.setWeightSourceQuality(algorithmConfig.weightSourceQuality());
    config.setWeightPopularity(algorithmConfig.weightPopularity());
    config.setWeightTopic(algorithmConfig.weightTopic());

    List<Topic> resolvedTopics = topicRepository.findAllById(algorithmConfig.topics());

    config.getTopics().clear();
    config.getTopics().addAll(resolvedTopics);

    if (resolvedTopics.isEmpty()) {
      config.setTopicPreferenceEmbedding(null);
    } else {
      List<String> topicIds = resolvedTopics.stream().map(Topic::getTopicId).toList();
      List<Float> vectorList = topicRepository.calculateTopicPreferenceVector(topicIds);
      config.setTopicPreferenceEmbedding(VectorUtils.fromList(vectorList));
    }

    return algorithmMapper.toConfig(algorithmRepository.save(config));
  }

  public AlgorithmConfig getOrCreateConfig(User user) {
    return algorithmRepository
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              AlgorithmConfig defaultConfig =
                  AlgorithmConfig.builder()
                      .user(user)
                      .weightInteraction(70)
                      .weightRecency(60)
                      .weightExploration(70)
                      .weightSourceQuality(50)
                      .weightPopularity(50)
                      .weightTopic(40)
                      .topics(new HashSet<>())
                      .build();

              return algorithmRepository.save(defaultConfig);
            });
  }
}

package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.request.AlgorithmConfigRequest;
import at.ac.tuwien.inso.ctrl.algorithm.dto.response.AlgorithmConfigResponse;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.mapper.AlgorithmMapper;
import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlgorithmService {
  private final AlgorithmRepository algorithmRepository;
  private final AlgorithmMapper algorithmMapper;
  private final TopicRepository topicRepository;

  @Transactional
  public AlgorithmConfigResponse getConfig(User currentUser) {
    AlgorithmConfig config = getOrCreateConfig(currentUser);

    return algorithmMapper.toConfig(config);
  }

  @Transactional
  public AlgorithmConfigResponse updateConfig(
      User currentUser, AlgorithmConfigRequest algorithmConfig) {
    AlgorithmConfig config = getOrCreateConfig(currentUser);

    config.setWeightRecency(algorithmConfig.weightRecency());
    config.setWeightExploration(algorithmConfig.weightExploration());
    config.setWeightSourceQuality(algorithmConfig.weightSourceQuality());
    config.setWeightTrending(algorithmConfig.weightTrending());
    config.setWeightTopic(algorithmConfig.weightTopic());

    List<Topic> resolvedTopics = topicRepository.findAllById(algorithmConfig.topics());

    config.getTopics().clear();
    config.getTopics().addAll(resolvedTopics);

    return algorithmMapper.toConfig(algorithmRepository.save(config));
  }

  private AlgorithmConfig getOrCreateConfig(User user) {
    return algorithmRepository
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              AlgorithmConfig defaultConfig =
                  AlgorithmConfig.builder()
                      .user(user)
                      .weightRecency(50)
                      .weightExploration(50)
                      .weightSourceQuality(50)
                      .weightTrending(50)
                      .weightTopic(50)
                      .build();

              return algorithmRepository.save(defaultConfig);
            });
  }
}

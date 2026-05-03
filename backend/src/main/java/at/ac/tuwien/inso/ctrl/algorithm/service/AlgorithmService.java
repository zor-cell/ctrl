package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.AlgorithmConfigResponse;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.mapper.AlgorithmMapper;
import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlgorithmService {
  private final AlgorithmRepository algorithmRepository;
  private final AlgorithmMapper algorithmMapper;

  @Transactional()
  public AlgorithmConfigResponse getConfig(User currentUser) {
    var config = getConfigInternal(currentUser.getId());

    return algorithmMapper.toConfig(config);
  }

  @Transactional()
  public AlgorithmConfigResponse updateAlgorithmConfig(
      User currentUser, AlgorithmConfigResponse algorithmConfig) {
    var config = getConfigInternal(currentUser.getId());

    config.setWeightRecency(algorithmConfig.weightRecency());
    config.setWeightExploration(algorithmConfig.weightExploration());
    config.setWeightSourceQuality(algorithmConfig.weightSource());
    config.setWeightTrending(algorithmConfig.weightTrending());
    config.setWeightTopic(algorithmConfig.weightTopic());

    return algorithmMapper.toConfig(algorithmRepository.save(config));
  }

  private AlgorithmConfig getConfigInternal(UUID userId) {
    return algorithmRepository
        .findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Config not found"));
  }
}

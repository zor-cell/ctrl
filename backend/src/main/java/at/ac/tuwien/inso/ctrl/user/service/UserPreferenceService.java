package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.embedding.VectorUtils;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

  private final UserRepository userRepository;

  /**
   * Recalculates the user's interaction preference vector and saves it. Runs asynchronously so it
   * doesn't block whatever triggered it.
   */
  @Async
  @Transactional
  public void updateInteractionVectorAsync(UUID userId) {
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) return;

    List<Float> vectorList = userRepository.calculateUserInteractionPreferenceVector(userId);

    float[] vector = VectorUtils.fromList(vectorList);

    user.setInteractionPreferenceEmbedding(vector);
    userRepository.save(user);
  }
}

package at.ac.tuwien.inso.ctrl.auth.scheduler;

import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthScheduler {
  private final PasswordResetRepository passwordResetRepository;
  private final EmailVerificationRepository emailVerificationRepository;

  @Scheduled(cron = "0 55 1 * * *")
  @Transactional
  public void deleteExpiredPasswordResets() {
    log.info("Deleting expired password resets...");
    try {
      int deletedCount = passwordResetRepository.deleteExpiredTokens();
      log.info("Deleted {} expired password resets", deletedCount);
    } catch (Exception e) {
      log.error("Error deleting expired password resets", e);
    }
  }

  @Scheduled(cron = "0 55 1 * * *")
  public void deleteExpiredVerificationTokens() {
    log.info("Deleting expired tokens...");
    try {
      int deletedCount = emailVerificationRepository.deleteExpiredTokens();
      log.info("Deleted {} expired password resets", deletedCount);
    } catch (Exception e) {
      log.error("Error deleting expired tokens", e);
    }
  }
}

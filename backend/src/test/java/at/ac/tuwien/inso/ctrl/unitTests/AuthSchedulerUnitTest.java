package at.ac.tuwien.inso.ctrl.unitTests;

import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import at.ac.tuwien.inso.ctrl.auth.scheduler.AuthScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthSchedulerUnitTest {
  @Mock private PasswordResetRepository passwordResetRepository;
  @Mock
  private JwtDecoder jwtDecoder;
  @Mock private EmailVerificationRepository emailVerificationRepository;

  @InjectMocks
  private AuthScheduler authScheduler;

  @Test
  void deleteExpiredPasswordReset_whenRepositoryThrows_doesNotPropagate() {
    when(passwordResetRepository.deleteExpiredTokens())
        .thenThrow(new RuntimeException("db error"));

    assertDoesNotThrow(() -> authScheduler.deleteExpiredPasswordResets());
    verify(passwordResetRepository).deleteExpiredTokens();

  }

  @Test
  void deleteExpiredVerificationTokens_whenRepositoryThrows_doesNotPropagate() {
    when(emailVerificationRepository.deleteExpiredTokens())
        .thenThrow(new RuntimeException("db error"));

    assertDoesNotThrow(() -> authScheduler.deleteExpiredVerificationTokens());
    verify(emailVerificationRepository).deleteExpiredTokens();
  }
}

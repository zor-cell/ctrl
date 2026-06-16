package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.auth.entity.EmailVerification;
import at.ac.tuwien.inso.ctrl.auth.entity.PasswordReset;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import at.ac.tuwien.inso.ctrl.auth.scheduler.AuthScheduler;
import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class AuthSchedulerIntegrationTest {
  @Autowired private AuthScheduler authScheduler;
  @Autowired private PasswordResetRepository passwordResetRepository;
  @Autowired private UserRepository userRepository;

  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private GmailEmailService gmailEmailService;

  private User testUser;
  @Autowired private EmailVerificationRepository emailVerificationRepository;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setUsername("scheduler-user");
    testUser.setPassword("pw");
    testUser.setEmail("scheduler-user@test.com");
    testUser.setName("Scheduler User");
    testUser.setEmailVerified(true);
    testUser.setBirthdate(LocalDate.of(1990, 1, 1));
    userRepository.save(testUser);
  }

  @AfterEach
  void tearDown() {
    emailVerificationRepository.deleteAll();
    passwordResetRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void deleteExpiredPasswordResets_deletesOnlyExpiredTokens() {
    PasswordReset expiredToken = new PasswordReset();
    expiredToken.setToken("expired-token");
    expiredToken.setUser(testUser);
    expiredToken.setExpiresAt(LocalDateTime.now().minusMinutes(10));
    expiredToken.setUsed(false);

    PasswordReset validToken = new PasswordReset();
    validToken.setToken("valid-token");
    validToken.setUser(testUser);
    validToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    validToken.setUsed(false);

    passwordResetRepository.saveAll(List.of(expiredToken, validToken));

    authScheduler.deleteExpiredPasswordResets();

    assertEquals(1, passwordResetRepository.count());
    assertTrue(passwordResetRepository.findByToken("valid-token").isPresent());
  }

  @Test
  void deleteExpiredVerificationTokens_deletesOnlyExpiredTokens() {
    EmailVerification expiredToken = new EmailVerification();
    expiredToken.setUser(testUser);
    expiredToken.setExpiryTime(LocalDateTime.now().minusMinutes(10));
    expiredToken.setInvalid(true);

    EmailVerification validToken = new EmailVerification();
    validToken.setUser(testUser);
    validToken.setExpiryTime(LocalDateTime.now().plusMinutes(10));
    validToken.setInvalid(false);

    List<EmailVerification> saved =
        emailVerificationRepository.saveAll(List.of(validToken, expiredToken));
    EmailVerification savedValidToken = saved.get(0);

    authScheduler.deleteExpiredVerificationTokens();

    assertEquals(1, emailVerificationRepository.count());
    assertTrue(emailVerificationRepository.findById(savedValidToken.getId()).isPresent());
  }
}

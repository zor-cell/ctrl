package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.auth.dto.PasswordResetEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.entity.PasswordReset;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.auth.service.ResetPasswordService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thymeleaf.TemplateEngine;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class ResetPasswordServiceIntegrationTest {

  private static User testUser;
  private static ResetPasswordService resetPasswordService;

  @Autowired private TemplateEngine templateEngine;

  @Autowired private PasswordResetRepository passwordResetRepository;

  @Autowired private UserRepository userRepository;

  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private GmailEmailService gmailEmailService;

  @Autowired private PasswordEncoder passwordEncoder;

  @AfterEach
  public void tearDown() {
    passwordResetRepository.deleteAll();
    userRepository.deleteAll();
  }

  @BeforeEach
  public void setUp() {
    resetPasswordService =
        new ResetPasswordService(
            passwordEncoder,
            gmailEmailService,
            templateEngine,
            userRepository,
            passwordResetRepository);

    testUser = new User();
    testUser.setUsername("testUser");
    testUser.setPassword("password");
    testUser.setEmail("test_user@test.com");
    testUser.setName("Test User");
    testUser.setEmailVerified(true);
    testUser.setBirthdate(LocalDate.of(1990, 1, 1));
    userRepository.save(testUser);
  }

  @Test
  public void testSendPasswordReset_withValidData_shouldSendPasswordResetEmail() throws Exception {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest(testUser.getEmail());

    resetPasswordService.sendPasswordResetEmail(request);

    Mockito.verify(gmailEmailService)
        .sendHtmlEmail(
            Mockito.eq(testUser.getEmail()),
            Mockito.eq("Reset CTRL Password!"),
            Mockito.anyString());
  }

  @Test
  public void testSendPasswordReset_withValidData_shouldContainPersistData() {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest(testUser.getEmail());

    resetPasswordService.sendPasswordResetEmail(request);

    Optional<PasswordReset> resetOptional =
        passwordResetRepository.findPasswordResetByUser(testUser);

    assertTrue(resetOptional.isPresent());
    PasswordReset reset = resetOptional.get();
    assertTrue(reset.getUser().getId().equals(testUser.getId()));
    assertFalse(reset.isUsed());
    assertNotNull(reset.getToken());
  }

  @Test
  public void
      testSendPasswordReset_whenGmailServiceThrowsMailException_shouldThrowCommunicationException()
          throws Exception {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest(testUser.getEmail());

    Mockito.doThrow(new MailException("Mail server error") {})
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> resetPasswordService.sendPasswordResetEmail(request));

    assertTrue(
        exception
            .getMessage()
            .contains("Failed to send email. You can register again in 2 minutes."));
  }

  @Test
  public void
      testSendPasswordReset_whenGmailServiceThrowsException_shouldThrowCommunicationException()
          throws Exception {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest(testUser.getEmail());

    Mockito.doThrow(new IOException("Failed to create mime message"))
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> resetPasswordService.sendPasswordResetEmail(request));

    assertTrue(exception.getMessage().contains("Failed to send or create email"));
  }

  @Test
  public void testSendPasswordReset_withInvalidEmail_shouldNotPersistData() {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest("wrongEmail@gmail.com");

    resetPasswordService.sendPasswordResetEmail(request);

    Optional<PasswordReset> resetOptional =
        passwordResetRepository.findPasswordResetByUser(testUser);

    assertFalse(resetOptional.isPresent());
  }

  @Test
  public void testSendPasswordReset_withInvalidEmail_shouldNotSendEmail() throws Exception {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest("wrongEmail@gmail.com");

    resetPasswordService.sendPasswordResetEmail(request);
    Mockito.verify(gmailEmailService, Mockito.never())
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
  }
}

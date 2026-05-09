package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.auth.dto.PasswordResetEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.entity.PasswordReset;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import at.ac.tuwien.inso.ctrl.auth.service.ResetPasswordService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thymeleaf.TemplateEngine;

@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
public class ResetPasswordServiceIntegrationTest {

  private static JavaMailSender mailSender;
  private static User testUser;
  private static ResetPasswordService resetPasswordService;

  @Autowired private TemplateEngine templateEngine;

  @Autowired private PasswordResetRepository passwordResetRepository;

  @Autowired private UserRepository userRepository;

  @MockitoBean private JwtDecoder jwtDecoder;
  @Autowired private PasswordEncoder passwordEncoder;

  @AfterEach
  public void tearDown() {
    passwordResetRepository.deleteAll();
    userRepository.deleteAll();
  }

  @BeforeEach
  public void setUp() {
    mailSender = TestConfig.javaMailSender();
    resetPasswordService =
        new ResetPasswordService(
            passwordEncoder, mailSender, templateEngine, userRepository, passwordResetRepository);

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
  public void testSendPasswordReset_withValidData_shouldSendPasswordResetEmail()
      throws MessagingException, IOException {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest(testUser.getEmail());

    resetPasswordService.sendPasswordResetEmail(request);

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    Mockito.verify(mailSender).send(captor.capture());

    MimeMessage mimeMessage = captor.getValue();
    Multipart multipart = (Multipart) mimeMessage.getContent();

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multipart.writeTo(byteArrayOutputStream);

    assertEquals(1, mimeMessage.getAllRecipients().length);
    assertEquals(testUser.getEmail(), mimeMessage.getAllRecipients()[0].toString());
    assertEquals("Reset CTRL Password!", mimeMessage.getSubject());
  }

  @Test
  public void testSendPasswordReset_withValidData_shouldContainPersistData()
      throws MessagingException, IOException {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest(testUser.getEmail());

    resetPasswordService.sendPasswordResetEmail(request);

    Optional<PasswordReset> resetOptional =
        passwordResetRepository.findPasswordResetByUser(testUser);

    assertTrue(resetOptional.isPresent());
    PasswordReset reset = resetOptional.get();
    assertEquals(testUser.getId(), reset.getUser().getId());
    assertFalse(reset.isUsed());
    assertNotNull(reset.getToken());
  }

  @Test
  public void
      testSendPasswordReset_whenMailSenderThrowsMailException_shouldThrowCommunicationException() {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest(testUser.getEmail());

    Mockito.doThrow(new org.springframework.mail.MailException("Mail server error") {})
        .when(mailSender)
        .send(Mockito.any(MimeMessage.class));

    resetPasswordService =
        new ResetPasswordService(
            passwordEncoder, mailSender, templateEngine, userRepository, passwordResetRepository);
    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> resetPasswordService.sendPasswordResetEmail(request));

    assertTrue(exception.getMessage().contains("Failed to send email. You can register again in 2 minutes."));
  }

  @Test
  public void
      testSendPasswordReset_whenMimeMessageHelperThrowsMessagingException_shouldThrowCommunicationException()
          throws MessagingException {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest(testUser.getEmail());

    Mockito.when(mailSender.createMimeMessage())
        .thenAnswer(
            new Answer<MimeMessage>() {
              @Override
              public MimeMessage answer(org.mockito.invocation.InvocationOnMock invocation)
                  throws MessagingException {
                throw new MessagingException("Failed to create mime message");
              }
            });

    resetPasswordService =
        new ResetPasswordService(
            passwordEncoder, mailSender, templateEngine, userRepository, passwordResetRepository);
    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> resetPasswordService.sendPasswordResetEmail(request));

    assertTrue(exception.getMessage().contains("Failed to create email."));
  }

  @Test
  public void testSendPasswordReset_withInvalidEmail_shouldNotPersistData()
      throws MessagingException, IOException {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest("wrongEmail@gmail.com");

    resetPasswordService.sendPasswordResetEmail(request);

    Optional<PasswordReset> resetOptional =
        passwordResetRepository.findPasswordResetByUser(testUser);

    assertFalse(resetOptional.isPresent());
  }

  @Test
  public void testSendPasswordReset_withInvalidEmail_shouldNotSendEmail()
      throws MessagingException, IOException {
    PasswordResetEmailRequest request = new PasswordResetEmailRequest("wrongEmail@gmail.com");

    resetPasswordService.sendPasswordResetEmail(request);
    Mockito.verify(mailSender, Mockito.never()).send(Mockito.any(MimeMessage.class));
  }
}

package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.SendEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.entity.EmailVerification;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.service.EmailVerificationService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidEmailVerificationCodeException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.exception.UserAlreadyVerifiedException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
public class EmailVerificationServiceIntegrationTest {

  private static JavaMailSender mailSender;

  private static EmailVerificationService emailVerificationService;
  private static User testUserUnverified;
  private static User testUserVerified;

  @Autowired private TemplateEngine templateEngine;

  @Autowired private EmailVerificationRepository emailVerificationRepository;

  @Autowired private UserRepository userRepository;

  @MockitoBean private JwtDecoder jwtDecoder;

  @AfterEach
  public void tearDown() {
    emailVerificationRepository.deleteAll();
    userRepository.deleteAll();
  }

  @BeforeEach
  public void setUp() {
    mailSender = TestConfig.javaMailSender();
    emailVerificationService =
        new EmailVerificationService(
            mailSender, templateEngine, emailVerificationRepository, userRepository);
    ReflectionTestUtils.setField(emailVerificationService, "requireEmailVerification", true);
    testUserUnverified = new User();
    testUserUnverified.setUsername("testUserUnverified");
    testUserUnverified.setPassword("password");
    testUserUnverified.setEmail("test_unverrified@test.com");
    testUserUnverified.setName("Test User");
    testUserUnverified.setEmailVerified(false);
    testUserUnverified.setBirthdate(LocalDate.of(1990, 1, 1));
    userRepository.save(testUserUnverified);

    this.userRepository.save(testUserUnverified);

    testUserVerified = new User();
    testUserVerified.setUsername("testUserVerified");
    testUserVerified.setPassword("password");
    testUserVerified.setEmail("test_verified@test.com");
    testUserVerified.setName("Test User");
    testUserVerified.setEmailVerified(true);
    testUserVerified.setBirthdate(LocalDate.of(1990, 1, 1));
    userRepository.save(testUserVerified);
  }

  @Test
  public void testSendVerificationEmail_withUnverifiedEmail_shouldSendEmailWithCode()
      throws MessagingException, IOException {
    SendEmailRequest request =
        new SendEmailRequest(testUserUnverified.getUsername(), testUserUnverified.getEmail());
    emailVerificationService.sendVerificationEmail(request);

    ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    Mockito.verify(mailSender).send(messageCaptor.capture());

    MimeMessage mimeMessage = messageCaptor.getValue();
    Multipart multipart = (Multipart) mimeMessage.getContent();

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    multipart.writeTo(byteArrayOutputStream);

    assertEquals(1, mimeMessage.getAllRecipients().length);
    assertEquals(testUserUnverified.getEmail(), mimeMessage.getAllRecipients()[0].toString());
    assertEquals("Welcome to CTRL", mimeMessage.getSubject());

    Matcher matcher = getMatcher(byteArrayOutputStream);

    assertTrue(matcher.find());
    String code = matcher.group(1);

    assertNotEquals("111111", code);
  }

  @Test
  public void
      testSendVerificationEmail_withVerifiedEmail_shouldThrowUserAlreadyVerifiedException() {
    SendEmailRequest request =
        new SendEmailRequest(testUserVerified.getUsername(), testUserVerified.getEmail());
    assertThrows(
        UserAlreadyVerifiedException.class,
        () -> emailVerificationService.sendVerificationEmail(request));
  }

  @Test
  public void
      testSendVerificationEmail_whenMailSenderThrowsMailException_shouldThrowCommunicationException() {
    SendEmailRequest request =
        new SendEmailRequest(testUserUnverified.getUsername(), testUserUnverified.getEmail());

    Mockito.doThrow(new org.springframework.mail.MailException("Mail server error") {})
        .when(mailSender)
        .send(Mockito.any(MimeMessage.class));

    emailVerificationService =
        new EmailVerificationService(
            mailSender, templateEngine, emailVerificationRepository, userRepository);
    ReflectionTestUtils.setField(emailVerificationService, "requireEmailVerification", true);
    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> emailVerificationService.sendVerificationEmail(request));

    assertTrue(exception.getMessage().contains("Failed to send email"));
  }

  @Test
  public void
      testSendVerificationEmail_whenMimeMessageHelperThrowsMessagingException_shouldThrowCommunicationException()
          throws MessagingException {
    SendEmailRequest request =
        new SendEmailRequest(testUserUnverified.getUsername(), testUserUnverified.getEmail());

    Mockito.when(mailSender.createMimeMessage())
        .thenAnswer(
            new Answer<MimeMessage>() {
              @Override
              public MimeMessage answer(org.mockito.invocation.InvocationOnMock invocation)
                  throws MessagingException {
                throw new MessagingException("Failed to create mime message");
              }
            });

    emailVerificationService =
        new EmailVerificationService(
            mailSender, templateEngine, emailVerificationRepository, userRepository);
    ReflectionTestUtils.setField(emailVerificationService, "requireEmailVerification", true);

    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> emailVerificationService.sendVerificationEmail(request));

    assertTrue(exception.getMessage().contains("Failed to create email."));
  }

  @Test
  public void testVerifyEmail_whenCodeValid_shouldVerifyUser() {
    EmailVerification verification = new EmailVerification();
    verification.setUser(testUserUnverified);
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().plusMinutes(1));
    emailVerificationRepository.save(verification);

    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "123456");
    EmailVerificationResponse response = emailVerificationService.verifyEmail(request);

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertFalse(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertTrue(userOptional.get().isEmailVerified());
    assertTrue(response.verification_ok());
  }

  @Test
  public void testVerifyEmail_whenCodeInvalid_shouldThrowInvalidEmailVerificationCodeException() {
    EmailVerification verification = new EmailVerification();
    verification.setUser(testUserUnverified);
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().plusMinutes(1));
    emailVerificationRepository.save(verification);

    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "wrongC");

    InvalidEmailVerificationCodeException exception =
        assertThrows(
            InvalidEmailVerificationCodeException.class,
            () -> emailVerificationService.verifyEmail(request));

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertTrue(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertFalse(userOptional.get().isEmailVerified());

    assertTrue(
        exception
            .getMessage()
            .contains("Verification failed. User does not exist or Token is invalid"));
  }

  @Test
  public void
      testVerifyEmail_whenCodeExpiredCode_shouldThrowInvalidEmailVerificationCodeException() {
    EmailVerification verification = new EmailVerification();
    verification.setUser(testUserUnverified);
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().minusMinutes(1));
    emailVerificationRepository.save(verification);

    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "wrongC");

    InvalidEmailVerificationCodeException exception =
        assertThrows(
            InvalidEmailVerificationCodeException.class,
            () -> emailVerificationService.verifyEmail(request));

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertTrue(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertFalse(userOptional.get().isEmailVerified());

    assertTrue(
        exception
            .getMessage()
            .contains("Verification failed. User does not exist or Token is invalid"));
  }

  @Test
  public void testVerifyEmail_whenUserNonExistant_shouldThrowResourceNotFoundException() {
    EmailVerification verification = new EmailVerification();
    verification.setUser(testUserUnverified);
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().minusMinutes(1));
    emailVerificationRepository.save(verification);

    EmailVerificationRequest request =
        new EmailVerificationRequest("I dont exist", verification.getCode());

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> emailVerificationService.verifyEmail(request));

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertTrue(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertFalse(userOptional.get().isEmailVerified());

    assertTrue(
        exception
            .getMessage()
            .contains("Verification failed. User does not exist or Token is invalid"));
  }

  @Test
  public void testVerifyEmail_whenVerificationNonExistant_shouldThrowResourceNotFoundException() {
    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "anycod");

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> emailVerificationService.verifyEmail(request));

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertFalse(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertFalse(userOptional.get().isEmailVerified());

    assertTrue(
        exception
            .getMessage()
            .contains("Verification failed. User does not exist or Token is invalid"));
  }

  private static Matcher getMatcher(ByteArrayOutputStream byteArrayOutputStream) {
    String htmlContent = byteArrayOutputStream.toString();

    Pattern pattern = Pattern.compile("<p class=\"code\"><span>(.*?)</span></p>");

    return pattern.matcher(htmlContent);
  }
}

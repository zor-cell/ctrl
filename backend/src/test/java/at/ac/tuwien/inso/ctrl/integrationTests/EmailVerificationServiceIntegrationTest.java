package at.ac.tuwien.inso.ctrl.integrationTests;

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
import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidEmailVerificationCodeException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.exception.UserAlreadyVerifiedException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class EmailVerificationServiceIntegrationTest {

  private static EmailVerificationService emailVerificationService;
  private static User testUserUnverified;
  private static User testUserVerified;

  @Autowired private TemplateEngine templateEngine;

  @Autowired private EmailVerificationRepository emailVerificationRepository;

  @Autowired private UserRepository userRepository;

  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private GmailEmailService gmailEmailService;

  @AfterEach
  public void tearDown() {
    emailVerificationRepository.deleteAll();
    userRepository.deleteAll();
  }

  @BeforeEach
  public void setUp() {
    emailVerificationService =
        new EmailVerificationService(
            gmailEmailService, templateEngine, emailVerificationRepository, userRepository);
    ReflectionTestUtils.setField(emailVerificationService, "requireEmailVerification", true);

    testUserUnverified = new User();
    testUserUnverified.setUsername("testUserUnverified");
    testUserUnverified.setPassword("password");
    testUserUnverified.setEmail("test_unverrified@test.com");
    testUserUnverified.setName("Test User");
    testUserUnverified.setEmailVerified(false);
    testUserUnverified.setBirthdate(LocalDate.of(1990, 1, 1));
    userRepository.save(testUserUnverified);

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
      throws Exception {
    SendEmailRequest request =
        new SendEmailRequest(testUserUnverified.getUsername(), testUserUnverified.getEmail());

    emailVerificationService.sendVerificationEmail(request);

    ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(gmailEmailService)
        .sendHtmlEmail(
            Mockito.eq(testUserUnverified.getEmail()),
            Mockito.eq("Welcome to CTRL"),
            htmlCaptor.capture());

    Matcher matcher = getMatcher(htmlCaptor.getValue());
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
  public void testSendVerificationEmail_withWrongEmail_shouldUsernameNotFoundExceptionException()
      throws MessagingException, IOException {
    SendEmailRequest request = new SendEmailRequest(testUserUnverified.getUsername(), "wrongEmail");
    emailVerificationService.sendVerificationEmail(request);

    Mockito.verify(gmailEmailService, Mockito.never())
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    assertTrue(emailVerificationRepository.findAll().isEmpty());
  }

  @Test
  public void testSendVerificationEmail_withWrongUsername_shouldUsernameNotFoundExceptionException()
      throws MessagingException, IOException {
    SendEmailRequest request = new SendEmailRequest("wrongUser", testUserUnverified.getEmail());
    emailVerificationService.sendVerificationEmail(request);

    Mockito.verify(gmailEmailService, Mockito.never())
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    assertTrue(emailVerificationRepository.findAll().isEmpty());
  }

  @Test
  public void
      testSendVerificationEmail_withUserEmailMissmatch_shouldThrowResourceNothingButNotSendEmail()
          throws MessagingException, IOException {
    SendEmailRequest request =
        new SendEmailRequest(testUserVerified.getUsername(), testUserUnverified.getEmail());
    emailVerificationService.sendVerificationEmail(request);

    Mockito.verify(gmailEmailService, Mockito.never())
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    assertTrue(emailVerificationRepository.findAll().isEmpty());
  }

  @Test
  public void
      testSendVerificationEmail_whenGmailServiceThrowsMailException_shouldThrowCommunicationException()
          throws Exception {
    SendEmailRequest request =
        new SendEmailRequest(testUserUnverified.getUsername(), testUserUnverified.getEmail());

    Mockito.doThrow(new MailException("Mail server error") {})
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> emailVerificationService.sendVerificationEmail(request));

    assertTrue(exception.getMessage().contains("Failed to send email"));
  }

  @Test
  public void
      testSendVerificationEmail_whenGmailServiceThrowsException_shouldThrowCommunicationException()
          throws Exception {
    SendEmailRequest request =
        new SendEmailRequest(testUserUnverified.getUsername(), testUserUnverified.getEmail());

    Mockito.doThrow(new IOException("Failed to create mime message"))
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> emailVerificationService.sendVerificationEmail(request));

    assertTrue(exception.getMessage().contains("Failed to send or create email."));
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

    assertThrows(
        InvalidEmailVerificationCodeException.class,
        () -> emailVerificationService.verifyEmail(request));

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertTrue(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertFalse(userOptional.get().isEmailVerified());
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

    assertThrows(
        InvalidEmailVerificationCodeException.class,
        () -> emailVerificationService.verifyEmail(request));

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertTrue(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertFalse(userOptional.get().isEmailVerified());
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

    assertThrows(
        ResourceNotFoundException.class, () -> emailVerificationService.verifyEmail(request));

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertTrue(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertFalse(userOptional.get().isEmailVerified());
  }

  @Test
  public void testVerifyEmail_whenVerificationNonExistant_shouldThrowResourceNotFoundException() {
    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "anycod");

    assertThrows(
        ResourceNotFoundException.class, () -> emailVerificationService.verifyEmail(request));

    Optional<EmailVerification> emailVerificationOptional =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    Optional<User> userOptional = userRepository.findById(testUserUnverified.getId());

    assertFalse(emailVerificationOptional.isPresent());
    assertTrue(userOptional.isPresent());
    assertFalse(userOptional.get().isEmailVerified());
  }

  private static Matcher getMatcher(String htmlContent) {
    Pattern pattern = Pattern.compile("<p class=\"code\"><span>(.*?)</span></p>");
    return pattern.matcher(htmlContent);
  }
}

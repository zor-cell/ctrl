package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.SendEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.entity.EmailVerification;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.service.EmailVerificationService;
import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidEmailVerificationCodeException;
import at.ac.tuwien.inso.ctrl.exception.UserAlreadyVerifiedException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationServiceUnitTest {
  @Mock private GmailEmailService gmailEmailService;
  @Mock private TemplateEngine templateEngine;

  @Mock private EmailVerificationRepository emailVerificationRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private EmailVerificationService emailVerificationService;

  private User testUser;
  private SendEmailRequest validSendEmailRequest;
  private EmailVerificationRequest validVerificationRequest;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(emailVerificationService, "requireEmailVerification", true);

    testUser =
        User.builder()
            .id(UUID.randomUUID())
            .username("testUser")
            .password("hashedPassword")
            .email("test@domain.com")
            .emailVerified(false)
            .build();

    validSendEmailRequest = new SendEmailRequest("testUser", "test@domain.com");
    validVerificationRequest = new EmailVerificationRequest("testUser", "123456");
  }

  @Test
  void sendVerificationEmail_ShouldSaveVerificationCode_WhenRequestIsValid() throws Exception {
    when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
    when(userRepository.findByEmail("test@domain.com")).thenReturn(Optional.of(testUser));
    when(templateEngine.process(eq("verificationTemplate"), any(Context.class)))
        .thenReturn("<html>verification</html>");

    emailVerificationService.sendVerificationEmail(validSendEmailRequest);

    verify(gmailEmailService)
        .sendHtmlEmail(eq("test@domain.com"), eq("Welcome to CTRL"), any(String.class));

    ArgumentCaptor<EmailVerification> verificationCaptor =
        ArgumentCaptor.forClass(EmailVerification.class);
    verify(emailVerificationRepository).save(verificationCaptor.capture());

    EmailVerification savedVerification = verificationCaptor.getValue();
    assertEquals(testUser.getId(), savedVerification.getUser().getId());
    assertNotNull(savedVerification.getCode());
    assertNotNull(savedVerification.getExpiryTime());
  }

  @Test
  void sendVerificationEmail_ShouldThrowNothingButNotSendEmail_WhenUsernameNotFound()
      throws Exception {
    when(userRepository.findByUsername("testUser")).thenReturn(Optional.empty());

    emailVerificationService.sendVerificationEmail(validSendEmailRequest);

    verify(gmailEmailService, never())
        .sendHtmlEmail(any(String.class), any(String.class), any(String.class));
  }

  @Test
  void sendVerificationEmail_ShouldThrowException_WhenUserAlreadyVerified() throws Exception {
    testUser.setEmailVerified(true);
    when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
    when(userRepository.findByEmail("test@domain.com")).thenReturn(Optional.of(testUser));

    assertThrows(
        UserAlreadyVerifiedException.class,
        () -> emailVerificationService.sendVerificationEmail(validSendEmailRequest));

    verify(gmailEmailService, never())
        .sendHtmlEmail(any(String.class), any(String.class), any(String.class));
  }

  @Test
  void sendVerificationEmail_ShouldThrowCommunicationException_WhenGmailServiceFails()
      throws Exception {
    when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
    when(userRepository.findByEmail("test@domain.com")).thenReturn(Optional.of(testUser));
    when(templateEngine.process(eq("verificationTemplate"), any(Context.class)))
        .thenReturn("<html>verification</html>");

    doThrow(new MailException("Mail server error") {})
        .when(gmailEmailService)
        .sendHtmlEmail(any(String.class), any(String.class), any(String.class));

    assertThrows(
        CommunicationException.class,
        () -> emailVerificationService.sendVerificationEmail(validSendEmailRequest));
  }

  @Test
  void verifyEmail_ShouldSetEmailVerifiedToTrue_WhenCodeIsValidAndNotExpired() {
    EmailVerification verification = new EmailVerification();
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().plusMinutes(5));
    verification.setUser(testUser);

    when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
    when(emailVerificationRepository.findEmailVerificationByUser_Id(testUser.getId()))
        .thenReturn(Optional.of(verification));

    EmailVerificationResponse response =
        emailVerificationService.verifyEmail(validVerificationRequest);

    assertTrue(response.verification_ok());
    verify(emailVerificationRepository).delete(verification);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertTrue(userCaptor.getValue().isEmailVerified());
  }

  @Test
  void verifyEmail_ShouldThrowException_WhenVerificationCodeIsExpired() {
    EmailVerification verification = new EmailVerification();
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().minusMinutes(1));
    verification.setUser(testUser);

    when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
    when(emailVerificationRepository.findEmailVerificationByUser_Id(testUser.getId()))
        .thenReturn(Optional.of(verification));

    assertThrows(
        InvalidEmailVerificationCodeException.class,
        () -> emailVerificationService.verifyEmail(validVerificationRequest));

    verify(emailVerificationRepository, never()).delete(any());
    verify(userRepository, never()).save(any());
  }
}

package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.AdminService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
public class AdminServiceIntegrationTest {
  private static AdminService adminService;
  private static User userToSuspend;

  @Autowired private TemplateEngine templateEngine;

  @Autowired private UserRepository userRepository;

  @MockitoBean private JwtDecoder jwtDecoder;
  @MockitoBean private GmailEmailService gmailEmailService;
  @MockitoBean private ReportService reportService;

  @AfterEach
  public void tearDown() {
    userRepository.deleteAll();
  }

  @BeforeEach
  public void before() {
    adminService =
        new AdminService(userRepository, gmailEmailService, templateEngine, reportService);
    ReflectionTestUtils.setField(adminService, "requireEmailVerification", true);
    userToSuspend =
        User.builder()
            .username("testuser")
            .email("test_user@gmail.com")
            .password("password")
            .name("Test User")
            .birthdate(LocalDate.of(1990, 1, 1))
            .build();
    userRepository.save(userToSuspend);
  }

  @Test
  public void deleteRequest_ShouldSuspendUserAndSendEmail() throws MessagingException, IOException {
    adminService.deleteRequest(userToSuspend.getUsername());

    User suspendedUser = userRepository.findByUsername(userToSuspend.getUsername()).orElseThrow();
    assertTrue(suspendedUser.isSuspended());
  }

  @Test
  public void deleteRequest_withInvalidUser_shouldThrowUsernameNotFound()
      throws MessagingException, IOException {
    assertThrows(UsernameNotFoundException.class, () -> adminService.deleteRequest("invalid_user"));
  }

  @Test
  public void deleteRequest_shouldSendEmail_withSubject() throws MessagingException, IOException {
    adminService.deleteRequest(userToSuspend.getUsername());

    ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(gmailEmailService)
        .sendHtmlEmail(
            Mockito.eq(userToSuspend.getEmail()),
            Mockito.eq("An action has been taken against you"),
            htmlCaptor.capture());
  }

  @Test
  public void deleteRequest_shouldNotSendEmailIfVarFalse() throws MessagingException, IOException {
    ReflectionTestUtils.setField(adminService, "requireEmailVerification", false);
    adminService.deleteRequest(userToSuspend.getUsername());

    Mockito.verify(gmailEmailService, Mockito.never())
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
  }

  @Test
  public void deleteRequest_shouldThrowCommunicationException_onMailException()
      throws MessagingException, IOException {
    Mockito.doThrow(new MailSendException("mail send failed"))
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> adminService.deleteRequest(userToSuspend.getUsername()));

    assertEquals("Failed to send email.", exception.getMessage());
  }

  @Test
  public void deleteRequest_shouldThrowCommunicationException_onMessagingException()
      throws MessagingException, IOException {
    Mockito.doThrow(new MessagingException("message creation failed"))
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> adminService.deleteRequest(userToSuspend.getUsername()));

    assertEquals("Failed to create email.", exception.getMessage());
  }

  @Test
  public void deleteRequest_shouldThrowCommunicationException_onIOException()
      throws MessagingException, IOException {
    Mockito.doThrow(new IOException("io failed"))
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

    CommunicationException exception =
        assertThrows(
            CommunicationException.class,
            () -> adminService.deleteRequest(userToSuspend.getUsername()));

    assertEquals("Failed to send or create email.", exception.getMessage());
  }
}

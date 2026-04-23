package at.ac.tuwien.inso.ctrl.integrationTests;

import at.ac.tuwien.inso.ctrl.auth.dto.SendEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.service.EmailVerificationService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
public class EmailVerificationIntegrationTest {

  private static JavaMailSender mailSender;
  private static EmailVerificationService emailVerificationService;
  private static User testUserUnverified;
  private static User testUserVerified;

  @Autowired
  private TemplateEngine templateEngine;

  @Autowired
  private EmailVerificationRepository emailVerificationRepository;

  @Autowired
  private UserRepository userRepository;



  @AfterEach
  public void tearDown() {
    emailVerificationRepository.deleteAll();
    userRepository.deleteAll();
  }



  @BeforeEach
  public void setUp() {
    mailSender = TestConfig.javaMailSender();
    emailVerificationService = new EmailVerificationService(mailSender, templateEngine, emailVerificationRepository, userRepository);
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
  public void testSendVerificationEmail_withUnverifiedEmail_shouldSendEmailWithCode() throws MessagingException, IOException {
    SendEmailRequest request = new SendEmailRequest(testUserUnverified.getUsername(), testUserUnverified.getEmail());
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

  }

}

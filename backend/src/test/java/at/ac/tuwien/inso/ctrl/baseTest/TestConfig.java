package at.ac.tuwien.inso.ctrl.baseTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public JavaMailSender javaMailSender() {
    JavaMailSender javaMailSender = mock(JavaMailSender.class);
    when(javaMailSender.createMimeMessage())
        .thenAnswer(invocation -> new MimeMessage((Session) null));
    return javaMailSender;
  }
}

package at.ac.tuwien.inso.ctrl.baseTest;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {


  @Bean
  @Primary
  public static JavaMailSender javaMailSender() {
    JavaMailSender javaMailSender = mock(JavaMailSender.class);
    when(javaMailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
    return javaMailSender;
  }
}

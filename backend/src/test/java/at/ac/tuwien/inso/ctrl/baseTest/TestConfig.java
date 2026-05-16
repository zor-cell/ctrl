package at.ac.tuwien.inso.ctrl.baseTest;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.gmail.Gmail;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

@TestConfiguration
public class TestConfig {

  @Bean
  public Gmail gmail() {
    return Mockito.mock(Gmail.class, RETURNS_DEEP_STUBS);
  }
}

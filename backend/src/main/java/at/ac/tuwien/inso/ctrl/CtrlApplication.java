package at.ac.tuwien.inso.ctrl;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication(exclude = OAuth2ResourceServerAutoConfiguration.class)
public class CtrlApplication {

  public static void main(String[] args) {
    SpringApplication.run(CtrlApplication.class, args);
  }

  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}

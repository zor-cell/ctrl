package at.ac.tuwien.inso.ctrl.baseTest;


import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
public abstract class AbstractTestContainer {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg18-trixie")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test")
      .withInitScript("init-pgvector.sql");


  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
  }
}

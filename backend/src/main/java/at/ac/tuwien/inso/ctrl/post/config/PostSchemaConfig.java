package at.ac.tuwien.inso.ctrl.post.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PostSchemaConfig {
  @Bean
  CommandLineRunner widenPostCaptionColumn(JdbcTemplate jdbcTemplate) {
    return args -> jdbcTemplate.execute("ALTER TABLE posts ALTER COLUMN caption TYPE text");
  }
}

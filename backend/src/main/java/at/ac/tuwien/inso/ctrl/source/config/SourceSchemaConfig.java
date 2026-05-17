package at.ac.tuwien.inso.ctrl.source.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SourceSchemaConfig {
  @Bean
  CommandLineRunner widenSingleSourceColumns(JdbcTemplate jdbcTemplate) {
    return args -> {
      jdbcTemplate.execute("ALTER TABLE single_source ALTER COLUMN title TYPE varchar(500)");
      jdbcTemplate.execute("ALTER TABLE single_source ALTER COLUMN url TYPE varchar(2048)");
    };
  }
}

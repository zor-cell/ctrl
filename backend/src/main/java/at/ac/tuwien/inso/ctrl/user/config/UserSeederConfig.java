package at.ac.tuwien.inso.ctrl.user.config;

import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class UserSeederConfig {

  @Bean
  CommandLineRunner backfillUserVerification(
      UserRepository userRepository, TransactionTemplate transactionTemplate) {
    return args ->
        transactionTemplate.executeWithoutResult(
            transactionStatus -> userRepository.backfillNullVerifiedUsers());
  }
}

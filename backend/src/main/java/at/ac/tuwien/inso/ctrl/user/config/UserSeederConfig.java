package at.ac.tuwien.inso.ctrl.user.config;

import at.ac.tuwien.inso.ctrl.user.dto.Role;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("!test")
public class UserSeederConfig {
  @Value("${application.seed.admin.username}")
  private String adminUsername;

  @Value("${application.seed.admin.password}")
  private String adminPassword;

  @Bean
  CommandLineRunner backfillUserVerification(
      UserRepository userRepository, TransactionTemplate transactionTemplate) {
    return args ->
        transactionTemplate.executeWithoutResult(
            transactionStatus -> userRepository.backfillNullVerifiedUsers());
  }

  @Bean
  CommandLineRunner seedAdminAndTestUsers(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TransactionTemplate transactionTemplate) {
    return args ->
        transactionTemplate.executeWithoutResult(
            transactionStatus -> {

              // create admin user
              if (!userRepository.existsByUsername(adminUsername)) {
                userRepository.save(
                    User.builder()
                        .username(adminUsername)
                        .password(passwordEncoder.encode(adminPassword))
                        .name("System Administrator")
                        .email("admin@system.local")
                        .isVerified(true)
                        .emailVerified(true)
                        .role(Role.ADMIN)
                        .build());
              }

              // create test users
              if (!userRepository.existsByUsername("test")
                  && !userRepository.existsByUsername("test1")) {
                userRepository.saveAll(
                    List.of(
                        User.builder()
                            .username("test")
                            .password(passwordEncoder.encode("test"))
                            .name("Alice Test")
                            .email("alice@test.local")
                            .isVerified(true)
                            .emailVerified(true)
                            .role(Role.USER)
                            .build(),
                        User.builder()
                            .username("test1")
                            .password(passwordEncoder.encode("test1"))
                            .name("Bob Test")
                            .email("bob@test.local")
                            .isVerified(false)
                            .emailVerified(true)
                            .role(Role.USER)
                            .build()));
              }
            });
  }
}

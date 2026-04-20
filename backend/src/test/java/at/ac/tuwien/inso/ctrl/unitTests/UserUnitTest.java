package at.ac.tuwien.inso.ctrl.unitTests;

import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.auth.service.AuthService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class UserUnitTest {

  @Autowired
  private AuthService authService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Transactional
  @Test
  public void should_register_one_user() {
    String username = "test";
    String password = "test";
    String email = "testMail@gmailcom";
    String name = "Test";
    LocalDate birthdate = LocalDate.of(2000, 1, 1);

    RegisterRequest registerRequest = new RegisterRequest(
        username,
        password,
        email,
        name,
        birthdate
        );

    authService.register(registerRequest);

    Optional<User> user = userRepository.findByUsername(username);

    assertNotNull(user);
    assertTrue(user.isPresent());
    assertEquals(email, user.get().getEmail());
    assertEquals(name, user.get().getName());
    assertTrue(passwordEncoder.matches(password, user.get().getPassword()));

    //clean up
    userRepository.delete(user.get());

  }
}

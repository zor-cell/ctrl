package at.ac.tuwien.inso.ctrl.unitTests;

import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.auth.service.AuthService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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

  @Autowired
  private UserService userService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

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

  @Transactional
  @Test
  public void test_conductUserCleanup_withUserOlderThanOneDayAndNotVerified() {
    //prepare db
    User user = new User();
    user.setUsername("test");
    user.setName("Test");
    user.setEmail("test@gmail.com");
    user.setPassword("12345678");
    user.setEmailVerified(false);
    user.setBirthdate(LocalDate.of(2000, 1, 1));
    user.setCreatedAt(LocalDate.now().minusDays(1).atTime(LocalTime.now().minusMinutes(1)).toInstant(java.time.ZoneOffset.UTC));

    userRepository.save(user);

    userService.conductUserCleanup();

    Optional<User> user_req = userRepository.findByEmail(user.getEmail());

    assertNotNull(user_req);
    assertTrue(user_req.isPresent());
    assertEquals(user.getEmail(), user_req.get().getEmail());
    assertEquals(user.getUsername(), user_req.get().getUsername());
    assertEquals(user.getPassword(), user_req.get().getPassword());
    assertEquals(user.getCreatedAt(), user_req.get().getCreatedAt());
    assertEquals(user.getBirthdate(), user_req.get().getBirthdate());
  }

  @Transactional
  @Test
  public void test_conductUserCleanup_withUserOlderThanOneDayAndVerified() {
    //prepare db
    User user = new User();
    user.setUsername("test");
    user.setName("Test");
    user.setEmail("test@gmail.com");
    user.setPassword("12345678");
    user.setEmailVerified(true);
    user.setBirthdate(LocalDate.of(2000, 1, 1));
    user.setCreatedAt(LocalDate.now().minusDays(1).atTime(LocalTime.now().minusMinutes(1)).toInstant(java.time.ZoneOffset.UTC));

    userRepository.save(user);

    userService.conductUserCleanup();

    Optional<User> user_req = userRepository.findByEmail(user.getEmail());

    assertNotNull(user_req);
    assertTrue(user_req.isPresent());
    assertEquals(user.getEmail(), user_req.get().getEmail());
    assertEquals(user.getUsername(), user_req.get().getUsername());
    assertEquals(user.getPassword(), user_req.get().getPassword());
    assertEquals(user.getCreatedAt(), user_req.get().getCreatedAt());
    assertEquals(user.getBirthdate(), user_req.get().getBirthdate());
  }

  @Transactional
  @Test
  public void test_conductUserCleanup_withUserYoungerThanOneDayAndNotVerified() {
    //prepare db
    User user = new User();
    user.setUsername("test");
    user.setName("Test");
    user.setEmail("test@gmail.com");
    user.setPassword("12345678");
    user.setEmailVerified(false);
    user.setBirthdate(LocalDate.of(2000, 1, 1));
    user.setCreatedAt(LocalDate.now().minusDays(1).atTime(LocalTime.now().plusMinutes(1)).toInstant(java.time.ZoneOffset.UTC));

    userRepository.save(user);

    userService.conductUserCleanup();

    Optional<User> user_req = userRepository.findByEmail(user.getEmail());

    assertNotNull(user_req);
    assertTrue(user_req.isPresent());
    assertEquals(user.getEmail(), user_req.get().getEmail());
    assertEquals(user.getUsername(), user_req.get().getUsername());
    assertEquals(user.getPassword(), user_req.get().getPassword());
    assertEquals(user.getCreatedAt(), user_req.get().getCreatedAt());
    assertEquals(user.getBirthdate(), user_req.get().getBirthdate());


  }
}

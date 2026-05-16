package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.auth.dto.EmailVerificationRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.LoginRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.SendEmailRequest;
import at.ac.tuwien.inso.ctrl.auth.entity.EmailVerification;
import at.ac.tuwien.inso.ctrl.auth.entity.PasswordReset;
import at.ac.tuwien.inso.ctrl.auth.repository.EmailVerificationRepository;
import at.ac.tuwien.inso.ctrl.auth.repository.PasswordResetRepository;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;
  @Autowired private PasswordResetRepository passwordResetRepository;

  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private EmailVerificationRepository emailVerificationRepository;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private User existingUser;
  private User testUserUnverified;
  private User testUserVerified;
  private final String sendEmailEndpoint = "/api/auth/email";
  private final String verifyEndpoint = "/api/auth/verify";

  private final String forgotPasswordEndpoint = "/api/auth/forgot-password";
  private final String resetPasswordEndpoint = "/api/auth/reset-password";

  private final String registerEndpoint = "/api/auth/register";
  private final String loginEndpoint = "/api/auth/login";

  @BeforeEach
  public void setUp() {
    userRepository.deleteAll();

    existingUser =
        User.builder()
            .username("integrationUser")
            .password(passwordEncoder.encode("securePass123!"))
            .name("Integration Test")
            .email("integration@test.com")
            .birthdate(LocalDate.of(1995, 5, 5))
            .emailVerified(true)
            .build();

    userRepository.save(existingUser);

    testUserUnverified = new User();
    testUserUnverified.setUsername("testUserUnverified");
    testUserUnverified.setPassword("password");
    testUserUnverified.setEmail("test_unverified@test.com");
    testUserUnverified.setName("Test User");
    testUserUnverified.setEmailVerified(false);
    testUserUnverified.setBirthdate(LocalDate.of(1990, 1, 1));
    userRepository.save(testUserUnverified);

    testUserVerified = new User();
    testUserVerified.setUsername("testUserVerified");
    testUserVerified.setPassword("password");
    testUserVerified.setEmail("test_verified@test.com");
    testUserVerified.setName("Test User");
    testUserVerified.setEmailVerified(true);
    testUserVerified.setBirthdate(LocalDate.of(1990, 1, 1));
    userRepository.save(testUserVerified);
  }

  @AfterEach
  public void tearDown() {

    emailVerificationRepository.deleteAll();
    passwordResetRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  public void register_shouldReturnBadRequest_WhenInvalidRequest() throws Exception {
    RegisterRequest invalidRequest =
        new RegisterRequest("", "", "not-an-email", "   ", LocalDate.of(2050, 1, 1));

    mockMvc
        .perform(
            post(registerEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    assertTrue(userRepository.findByEmail("not-an-email").isEmpty());
  }

  @Test
  public void register_shouldSaveUser_WhenRequestIsValid() throws Exception {
    RegisterRequest request =
        new RegisterRequest(
            "newUser", "newPassword123!", "new@test.com", "New User", LocalDate.of(2000, 1, 1));

    mockMvc
        .perform(
            post(registerEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    assertTrue(userRepository.existsByUsername("newUser"));
  }

  @Test
  public void register_shouldReturnConflict_WhenUsernameExists() throws Exception {
    RegisterRequest request =
        new RegisterRequest(
            existingUser.getUsername(),
            "newPassword123!",
            "unique@test.com",
            "New User",
            LocalDate.of(2000, 1, 1));

    mockMvc
        .perform(
            post(registerEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  public void register_shouldReturnConflict_WhenEmailExists() throws Exception {
    RegisterRequest request =
        new RegisterRequest(
            "completelyNewUsername",
            "newPassword123!",
            existingUser.getEmail(),
            "New User",
            LocalDate.of(2000, 1, 1));

    mockMvc
        .perform(
            post(registerEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  public void login_shouldReturnBadRequest_WhenInvalidRequest() throws Exception {
    LoginRequest invalidRequest = new LoginRequest("", "  ");

    mockMvc
        .perform(
            post(loginEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void login_shouldReturnJwt_WhenUserExists() throws Exception {
    LoginRequest request = new LoginRequest(existingUser.getUsername(), "securePass123!");

    mockMvc
        .perform(
            post(loginEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.token").isString());
  }

  @Test
  public void login_shouldReturnUnauthorized_WhenWrongPassword() throws Exception {
    LoginRequest request = new LoginRequest("integrationUser", "wrongPassword");

    mockMvc
        .perform(
            post(loginEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void login_shouldReturnForbidden_WhenEmailNotVerified() throws Exception {
    User unverifiedUser =
        User.builder()
            .username("unverifiedUser")
            .password(passwordEncoder.encode("securePass123!"))
            .name("Unverified")
            .email("unverified@test.com")
            .birthdate(LocalDate.of(1995, 5, 5))
            .emailVerified(false)
            .build();
    userRepository.save(unverifiedUser);

    LoginRequest request =
        new LoginRequest(unverifiedUser.getUsername(), unverifiedUser.getPassword());

    mockMvc
        .perform(
            post(loginEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  public void testResendVerificationEmail_withUnverifiedUser_shouldReturnCreated()
      throws Exception {
    SendEmailRequest request =
        new SendEmailRequest(testUserUnverified.getUsername(), testUserUnverified.getEmail());

    mockMvc
        .perform(
            post(sendEmailEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    Optional<EmailVerification> verification =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    assertTrue(verification.isPresent());
    assertNotNull(verification.get().getCode());
    assertNotNull(verification.get().getExpiryTime());
  }

  @Test
  public void testResendVerificationEmail_withVerifiedUser_shouldReturnBadRequest()
      throws Exception {
    SendEmailRequest request =
        new SendEmailRequest(testUserVerified.getUsername(), testUserVerified.getEmail());

    mockMvc
        .perform(
            post(sendEmailEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());

    Optional<EmailVerification> verification =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserVerified.getId());
    assertFalse(verification.isPresent());
  }

  @Test
  public void testResendVerificationEmail_withWrongUsername_shouldReturnCreated() throws Exception {
    SendEmailRequest request = new SendEmailRequest("wrongUsername", testUserUnverified.getEmail());

    mockMvc
        .perform(
            post(sendEmailEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    Optional<EmailVerification> verification =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    assertFalse(verification.isPresent());
  }

  @Test
  public void testResendVerificationEmail_withWrongEmail_shouldReturnCreated() throws Exception {
    SendEmailRequest request =
        new SendEmailRequest(testUserUnverified.getUsername(), "wrong@email.com");

    mockMvc
        .perform(
            post(sendEmailEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    Optional<EmailVerification> verification =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    assertFalse(verification.isPresent());
  }

  @Test
  public void
      testResendVerificationEmail_withEmailUsernamesMismatch_shouldReturnCreatedButNotSendEmail()
          throws Exception {
    SendEmailRequest request =
        new SendEmailRequest(testUserVerified.getUsername(), testUserUnverified.getEmail());

    mockMvc
        .perform(
            post(sendEmailEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    Optional<EmailVerification> verification =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    assertFalse(verification.isPresent());
  }

  @Test
  public void testResendVerificationEmail_withMissingUsername_shouldReturnCreated()
      throws Exception {
    String invalidRequest = "{\"email\": \"test@test.com\"}";

    mockMvc
        .perform(post(sendEmailEndpoint).contentType("application/json").content(invalidRequest))
        .andExpect(status().isCreated());

    Optional<EmailVerification> verification =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    assertFalse(verification.isPresent());
  }

  @Test
  public void testResendVerificationEmail_withMissingEmail_shouldReturnCreated() throws Exception {
    String invalidRequest = "{\"username\": \"testuser\"}";

    mockMvc
        .perform(post(sendEmailEndpoint).contentType("application/json").content(invalidRequest))
        .andExpect(status().isCreated());

    Optional<EmailVerification> verification =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    assertFalse(verification.isPresent());
  }

  @Test
  public void testVerifyEmail_withValidCode_shouldReturnOkAndVerificationOk() throws Exception {
    EmailVerification verification = new EmailVerification();
    verification.setUser(testUserUnverified);
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().plusMinutes(10));
    emailVerificationRepository.save(verification);

    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "123456");

    mockMvc
        .perform(
            post(verifyEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verification_ok").value(true));

    Optional<EmailVerification> ver =
        emailVerificationRepository.findEmailVerificationByUser_Id(testUserUnverified.getId());
    assertFalse(ver.isPresent());

    Optional<User> updatedUserOpt = userRepository.findById(testUserUnverified.getId());
    assertTrue(updatedUserOpt.isPresent());
    assertTrue(updatedUserOpt.get().isEmailVerified());
  }

  @Test
  public void testVerifyEmail_withInvalidCode_shouldReturnUnAuthorized() throws Exception {
    EmailVerification verification = new EmailVerification();
    verification.setUser(testUserUnverified);
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().plusMinutes(10));
    emailVerificationRepository.save(verification);

    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "wrongcode");

    mockMvc
        .perform(
            post(verifyEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());

    User unverifiedUser = userRepository.findById(testUserUnverified.getId()).orElse(null);
    assertNotNull(unverifiedUser);
    assertFalse(unverifiedUser.isEmailVerified());
  }

  @Test
  public void testVerifyEmail_withExpiredCode_shouldReturnBadUnauthorized() throws Exception {
    EmailVerification verification = new EmailVerification();
    verification.setUser(testUserUnverified);
    verification.setCode("123456");
    verification.setExpiryTime(LocalDateTime.now().minusMinutes(1));
    emailVerificationRepository.save(verification);

    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "123456");

    mockMvc
        .perform(
            post(verifyEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());

    User unverifiedUser = userRepository.findById(testUserUnverified.getId()).orElse(null);
    assertNotNull(unverifiedUser);
    assertFalse(unverifiedUser.isEmailVerified());
  }

  @Test
  public void testVerifyEmail_withNonExistentUser_shouldReturnNotFound() throws Exception {
    EmailVerificationRequest request = new EmailVerificationRequest("nonexistent", "123456");

    mockMvc
        .perform(
            post(verifyEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());

    User unverifiedUser = userRepository.findById(testUserUnverified.getId()).orElse(null);
    assertNotNull(unverifiedUser);
    assertFalse(unverifiedUser.isEmailVerified());
  }

  @Test
  public void testVerifyEmail_withUserButNoVerification_shouldReturnNotFound() throws Exception {
    EmailVerificationRequest request =
        new EmailVerificationRequest(testUserUnverified.getUsername(), "123456");

    mockMvc
        .perform(
            post(verifyEndpoint)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());

    User unverifiedUser = userRepository.findById(testUserUnverified.getId()).orElse(null);
    assertNotNull(unverifiedUser);
    assertFalse(unverifiedUser.isEmailVerified());
  }

  @Test
  public void testVerifyEmail_withMissingUsername_shouldReturnNotFound() throws Exception {
    String invalidRequest = "{\"code\": \"123456\"}";

    mockMvc
        .perform(post(verifyEndpoint).contentType("application/json").content(invalidRequest))
        .andExpect(status().isNotFound());

    User unverifiedUser = userRepository.findById(testUserUnverified.getId()).orElse(null);
    assertNotNull(unverifiedUser);
    assertFalse(unverifiedUser.isEmailVerified());
  }

  @Test
  public void testVerifyEmail_withMissingCode_shouldReturnNotFound() throws Exception {
    String invalidRequest = "{\"username\": \"testuser\"}";

    mockMvc
        .perform(post(verifyEndpoint).contentType("application/json").content(invalidRequest))
        .andExpect(status().isNotFound());

    User unverifiedUser = userRepository.findById(testUserUnverified.getId()).orElse(null);
    assertNotNull(unverifiedUser);
    assertFalse(unverifiedUser.isEmailVerified());
  }

  @Test
  public void testSendPasswordReset_with_validEmail_shouldReturnCreated() throws Exception {
    String validRequest = "{\"email\": \"" + existingUser.getEmail() + "\"}";

    mockMvc
        .perform(post(forgotPasswordEndpoint).contentType("application/json").content(validRequest))
        .andExpect(status().isCreated());

    Optional<PasswordReset> reset = passwordResetRepository.findPasswordResetByUser(existingUser);
    assertTrue(reset.isPresent());
    PasswordReset passwordReset = reset.get();
    assertNotNull(passwordReset);
    assertFalse(passwordReset.isUsed());
    assertFalse(passwordReset.getExpiresAt().isBefore(LocalDateTime.now()));
  }

  @Test
  public void testSendPasswordReset_withInvalidEmail_shouldReturnCreated() throws Exception {
    String validRequest = "{\"email\": \" invalidEmail@gmail.com\"}";

    mockMvc
        .perform(post(forgotPasswordEndpoint).contentType("application/json").content(validRequest))
        .andExpect(status().isCreated());

    Optional<PasswordReset> reset = passwordResetRepository.findPasswordResetByUser(existingUser);
    assertFalse(reset.isPresent());
  }

  @Test
  public void testResetPassword_with_validEmailAndToken_shouldReturnCreated() throws Exception {
    PasswordReset passwordReset = new PasswordReset();
    passwordReset.setUser(existingUser);
    passwordReset.setToken("valid-token");
    passwordReset.setExpiresAt(LocalDateTime.now().plusMinutes(2));
    passwordReset.setUsed(false);
    passwordResetRepository.save(passwordReset);

    String validRequest =
        "{\"token\": \"" + passwordReset.getToken() + "\"," + "\"password\": \"newPassword\"}";

    mockMvc
        .perform(post(resetPasswordEndpoint).contentType("application/json").content(validRequest))
        .andExpect(status().isCreated());

    Optional<PasswordReset> reset = passwordResetRepository.findPasswordResetByUser(existingUser);
    assertTrue(reset.isPresent());
    PasswordReset newPasswordReset = reset.get();
    assertNotNull(newPasswordReset);
    assertTrue(newPasswordReset.isUsed());

    Optional<User> updatedUserOpt = userRepository.findById(existingUser.getId());
    assertTrue(updatedUserOpt.isPresent());
    User updatedUser = updatedUserOpt.get();
    assertTrue(passwordEncoder.matches("newPassword", updatedUser.getPassword()));
  }

  @Test
  public void testResetPassword_withNonExistentToken_shouldReturnUnauthorized() throws Exception {

    String validRequest =
        "{\"token\": \"randomToken\"," + "\"password\": \"" + existingUser.getPassword() + "\"}";

    mockMvc
        .perform(post(resetPasswordEndpoint).contentType("application/json").content(validRequest))
        .andExpect(status().isUnauthorized());

    Optional<PasswordReset> reset = passwordResetRepository.findPasswordResetByUser(existingUser);
    assertFalse(reset.isPresent());

    Optional<User> updatedUserOpt = userRepository.findById(existingUser.getId());
    assertTrue(updatedUserOpt.isPresent());
    User updatedUser = updatedUserOpt.get();
    assertEquals(existingUser.getPassword(), updatedUser.getPassword());
  }

  @Test
  public void testResetPassword_withUsedToken_shouldReturnUnauthorized() throws Exception {
    PasswordReset passwordReset = new PasswordReset();
    passwordReset.setUser(existingUser);
    passwordReset.setToken("valid-token");
    passwordReset.setExpiresAt(LocalDateTime.now().plusMinutes(2));
    passwordReset.setUsed(true);
    passwordResetRepository.save(passwordReset);

    String validRequest =
        "{\"token\": \""
            + passwordReset.getToken()
            + "\","
            + "\"password\": \""
            + existingUser.getPassword()
            + "\"}";

    mockMvc
        .perform(post(resetPasswordEndpoint).contentType("application/json").content(validRequest))
        .andExpect(status().isUnauthorized());

    Optional<PasswordReset> reset = passwordResetRepository.findPasswordResetByUser(existingUser);
    assertTrue(reset.isPresent());

    Optional<User> updatedUserOpt = userRepository.findById(existingUser.getId());
    assertTrue(updatedUserOpt.isPresent());
    User updatedUser = updatedUserOpt.get();
    assertEquals(existingUser.getPassword(), updatedUser.getPassword());
  }

  @Test
  public void testResetPassword_withExpiredToken_shouldReturnUnauthorized() throws Exception {
    PasswordReset passwordReset = new PasswordReset();
    passwordReset.setUser(existingUser);
    passwordReset.setToken("valid-token");
    passwordReset.setExpiresAt(LocalDateTime.now().minusMinutes(2));
    passwordReset.setUsed(false);
    passwordResetRepository.save(passwordReset);

    String validRequest =
        "{\"token\": \""
            + passwordReset.getToken()
            + "\","
            + "\"password\": \""
            + existingUser.getPassword()
            + "\"}";

    mockMvc
        .perform(post(resetPasswordEndpoint).contentType("application/json").content(validRequest))
        .andExpect(status().isUnauthorized());

    Optional<PasswordReset> reset = passwordResetRepository.findPasswordResetByUser(existingUser);
    assertTrue(reset.isPresent());

    Optional<User> updatedUserOpt = userRepository.findById(existingUser.getId());
    assertTrue(updatedUserOpt.isPresent());
    User updatedUser = updatedUserOpt.get();
    assertEquals(existingUser.getPassword(), updatedUser.getPassword());
  }
}

package at.ac.tuwien.inso.ctrl.integrationTests;

import at.ac.tuwien.inso.ctrl.auth.dto.LoginRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User existingUser;

    private final String registerEndpoint = "/api/auth/register";
    private final String loginEndpoint = "/api/auth/login";

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();

        existingUser = User.builder()
                .username("integrationUser")
                .password(passwordEncoder.encode("securePass123!"))
                .name("Integration Test")
                .email("integration@test.com")
                .birthdate(LocalDate.of(1995, 5, 5))
                .emailVerified(true)
                .build();

        userRepository.save(existingUser);
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    public void register_shouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest(
                "",
                "",
                "not-an-email",
                "   ",
                LocalDate.of(2050, 1, 1)
        );

        mockMvc.perform(post(registerEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        assertTrue(userRepository.findByEmail("not-an-email").isEmpty());
    }

    @Test
    public void register_shouldSaveUser_WhenRequestIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newUser",
                "newPassword123!",
                "new@test.com",
                "New User",
                LocalDate.of(2000, 1, 1)
        );

        mockMvc.perform(post(registerEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertTrue(userRepository.existsByUsername("newUser"));
    }

    @Test
    public void register_shouldReturnConflict_WhenUsernameExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                existingUser.getUsername(),
                "newPassword123!",
                "unique@test.com",
                "New User",
                LocalDate.of(2000, 1, 1)
        );

        mockMvc.perform(post(registerEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    public void register_shouldReturnConflict_WhenEmailExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "completelyNewUsername",
                "newPassword123!",
                existingUser.getEmail(),
                "New User",
                LocalDate.of(2000, 1, 1)
        );

        mockMvc.perform(post(registerEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }



    @Test
    public void login_shouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "  ");

        mockMvc.perform(post(loginEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void login_shouldReturnJwt_WhenUserExists() throws Exception {
        LoginRequest request = new LoginRequest(existingUser.getUsername(), "securePass123!");

        mockMvc.perform(post(loginEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    public void login_shouldReturnUnauthorized_WhenWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("integrationUser", "wrongPassword");

        mockMvc.perform(post(loginEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void login_shouldReturnForbidden_WhenEmailNotVerified() throws Exception {
        User unverifiedUser = User.builder()
                .username("unverifiedUser")
                .password(passwordEncoder.encode("securePass123!"))
                .name("Unverified")
                .email("unverified@test.com")
                .birthdate(LocalDate.of(1995, 5, 5))
                .emailVerified(false)
                .build();
        userRepository.save(unverifiedUser);

        LoginRequest request = new LoginRequest(unverifiedUser.getUsername(), unverifiedUser.getPassword());

        mockMvc.perform(post(loginEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
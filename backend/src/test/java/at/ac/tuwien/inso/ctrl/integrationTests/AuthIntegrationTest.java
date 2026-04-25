package at.ac.tuwien.inso.ctrl.integrationTests;

import at.ac.tuwien.inso.ctrl.auth.dto.LoginRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User existingUser;

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
    public void should_RegisterNewUser_AndSaveToDatabase() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "newUser",
                "newPassword123!",
                "new@test.com",
                "New User",
                LocalDate.of(2000, 1, 1)
        );

        mockMvc.perform(post("/auth/register") // Passe den Pfad an deinen AuthController an!
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // Oder isCreated(), je nachdem was dein Controller zurückgibt

        // Verifizieren, dass der User wirklich in der echten DB gelandet ist
        assertTrue(userRepository.existsByUsername("newUser"));
    }

    @Test
    public void should_ReturnConflict_When_RegisteringWithExistingUsername() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "integrationUser", // Existiert bereits aus dem @BeforeEach
                "newPassword123!",
                "unique@test.com",
                "New User",
                LocalDate.of(2000, 1, 1)
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()); // Wir erwarten, dass der GlobalExceptionHandler greift
    }

    @Test
    public void should_LoginSuccessfully_AndReturnJwtToken() throws Exception {
        LoginRequest request = new LoginRequest("integrationUser", "securePass123!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty()) // Wir erwarten ein echtes JWT im Response
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    public void should_ReturnUnauthorized_When_LoginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("integrationUser", "wrongPassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Spring Security oder dein Controller sollte hier 401 Unauthorized werfen
                .andExpect(status().isUnauthorized());
    }
}
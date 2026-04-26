package at.ac.tuwien.inso.ctrl.integrationTests;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.dto.UpdatePasswordRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateProfileRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateUsernameRequest;
import at.ac.tuwien.inso.ctrl.user.dto.VerifyIdentityRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.IdAustriaOidcService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private IdAustriaOidcService idAustriaOidcService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User existingUser;
    private String validJwtToken;

    private final String meEndpoint = "/api/users/me";

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

        validJwtToken = jwtService.generateToken(existingUser);
    }

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    public void getUser_shouldReturnUserDetails_WhenAuthenticated() throws Exception {
        mockMvc.perform(get(meEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integrationUser"))
                .andExpect(jsonPath("$.email").value("integration@test.com"));
    }

    @Test
    public void getUser_shouldReturnUnauthorized_WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get(meEndpoint))
                .andExpect(status().isUnauthorized());
    }


    @Test
    public void updateUser_shouldUpdateProfile_WhenRequestIsValid() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "updated@test.com", LocalDate.of(1996, 6, 6));

        mockMvc.perform(put(meEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@test.com"));

        User dbUser = userRepository.findById(existingUser.getId()).orElseThrow();
        assertEquals("updated@test.com", dbUser.getEmail());
    }

    @Test
    public void updateUser_shouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        User secondUser = User.builder()
                .username("secondUser")
                .password("pass")
                .email("taken@test.com")
                .name("Second")
                .build();
        userRepository.save(secondUser);

        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "taken@test.com", LocalDate.of(1995, 5, 5));

        mockMvc.perform(put(meEndpoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }


    @Test
    public void updatePassword_shouldReturnNoContent_WhenOldPasswordIsCorrect() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("securePass123!", "newSecurePass123!");

        mockMvc.perform(put(meEndpoint + "/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Verify the new password hash is stored in the DB
        User dbUser = userRepository.findById(existingUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("newSecurePass123!", dbUser.getPassword()));
    }

    @Test
    public void updateUsername_shouldReturnNewToken_WhenUsernameIsAvailable() throws Exception {
        UpdateUsernameRequest request = new UpdateUsernameRequest("newAwesomeUsername");

        mockMvc.perform(put(meEndpoint + "/username")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        User dbUser = userRepository.findById(existingUser.getId()).orElseThrow();
        assertEquals("newAwesomeUsername", dbUser.getUsername());
    }

    @Test
    public void verifyUser_shouldUpdateVerifiedStatus_WhenIdAustriaSucceeds() throws Exception {
        existingUser.setVerified(false);
        userRepository.save(existingUser);

        VerifyIdentityRequest request = new VerifyIdentityRequest("valid-code");

        // Mock the external ID Austria JWT response
        Jwt mockJwt = mock(Jwt.class);
        when(idAustriaOidcService.getTokenFromCode("valid-code")).thenReturn(mockJwt);
        when(mockJwt.getClaimAsString("given_name")).thenReturn("Official");
        when(mockJwt.getClaimAsString("family_name")).thenReturn("Person");
        when(mockJwt.getClaimAsString("birthdate")).thenReturn("1980-08-08");

        mockMvc.perform(put(meEndpoint + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true));

        User dbUser = userRepository.findById(existingUser.getId()).orElseThrow();
        assertTrue(dbUser.isVerified());
        assertEquals("Official Person", dbUser.getName());
    }
}
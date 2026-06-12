package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.session.dto.SessionConfigDto;
import at.ac.tuwien.inso.ctrl.session.entity.SessionConfig;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.repository.SessionConfigRepository;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
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
public class SessionControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;
  @Autowired private SessionConfigRepository sessionConfigRepository;
  @Autowired private SessionRepository sessionRepository;

  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private User existingUser;
  private String validJwtToken;

  private final String sessionEndpoint = "/api/session";

  @BeforeEach
  public void setUp() {
    sessionRepository.deleteAll();
    sessionConfigRepository.deleteAll();
    userRepository.deleteAll();

    existingUser =
        User.builder()
            .username("sessionTestUser")
            .password(passwordEncoder.encode("securePass123!"))
            .name("Session Test")
            .email("session@test.com")
            .emailVerified(true)
            .build();

    userRepository.save(existingUser);
    validJwtToken = jwtService.generateToken(existingUser);
  }

  @AfterEach
  public void tearDown() {
    sessionRepository.deleteAll();
    sessionConfigRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  public void handleHeartbeat_shouldReturnOk_WhenLimitNotReached() throws Exception {
    mockMvc
        .perform(
            post(sessionEndpoint + "/heartbeat")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk());

    assertEquals(1, sessionRepository.count());
  }

  @Test
  public void handleHeartbeat_shouldReturnTooManyRequests_WhenLimitWasAlreadyExceeded()
      throws Exception {
    SessionConfig config =
        SessionConfig.builder()
            .user(existingUser)
            .timeLimitMinutes(10)
            .postLimit(100)
            .allowSnooze(false)
            .build();
    sessionConfigRepository.save(config);

    SessionHistory history =
        SessionHistory.builder()
            .user(existingUser)
            .start(LocalDateTime.now().minusMinutes(15))
            .seconds(900L)
            .postsViewed(0)
            .build();
    sessionRepository.save(history);

    mockMvc
        .perform(
            post(sessionEndpoint + "/heartbeat")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  public void handleHeartbeat_shouldReturnError_WhenLimitIsJustReached() throws Exception {
    SessionConfig config =
        SessionConfig.builder()
            .user(existingUser)
            .timeLimitMinutes(10)
            .postLimit(100)
            .allowSnooze(false)
            .build();
    sessionConfigRepository.save(config);

    SessionHistory history =
        SessionHistory.builder()
            .user(existingUser)
            .start(LocalDateTime.now().minusMinutes(10))
            .seconds(599L)
            .postsViewed(0)
            .build();
    sessionRepository.save(history);

    mockMvc
        .perform(
            post(sessionEndpoint + "/heartbeat")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  public void getConfig_shouldReturnDefaultConfig_WhenNoConfigExists() throws Exception {
    mockMvc
        .perform(
            get(sessionEndpoint + "/config")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeLimitMinutes").value(30))
        .andExpect(jsonPath("$.postLimit").value(100))
        .andExpect(jsonPath("$.allowSnooze").value(false));
  }

  @Test
  public void updateConfig_shouldApplyImmediately_WhenUnderLimit() throws Exception {
    SessionConfigDto request = new SessionConfigDto(45, 200, true);

    mockMvc
        .perform(
            put(sessionEndpoint + "/config")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeLimitMinutes").value(45))
        .andExpect(jsonPath("$.postLimit").value(200))
        .andExpect(jsonPath("$.allowSnooze").value(true))
        .andExpect(jsonPath("$.appliedImmediately").value(true));

    SessionConfig dbConfig =
        sessionConfigRepository.findByUserId(existingUser.getId()).orElseThrow();
    assertEquals(45, dbConfig.getTimeLimitMinutes());
    assertEquals(200, dbConfig.getPostLimit());
    assertEquals(true, dbConfig.isAllowSnooze());
  }

  @Test
  public void snooze_shouldReturnOk_WhenSnoozeAllowed() throws Exception {
    SessionConfig config =
        SessionConfig.builder()
            .user(existingUser)
            .timeLimitMinutes(30)
            .postLimit(100)
            .allowSnooze(true)
            .build();
    sessionConfigRepository.save(config);

    mockMvc
        .perform(
            post(sessionEndpoint + "/snooze")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk());

    SessionConfig dbConfig =
        sessionConfigRepository.findByUserId(existingUser.getId()).orElseThrow();
    assertNotNull(dbConfig.getLastSnooze());
  }

  @Test
  public void snooze_shouldReturnBadRequest_WhenSnoozeNotAllowed() throws Exception {
    SessionConfig config =
        SessionConfig.builder()
            .user(existingUser)
            .timeLimitMinutes(30)
            .postLimit(100)
            .allowSnooze(false)
            .build();
    sessionConfigRepository.save(config);

    mockMvc
        .perform(
            post(sessionEndpoint + "/snooze")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void handleHeartbeat_shouldReturnTooManyRequests_WhenPostLimitAlreadyExceeded()
      throws Exception {
    SessionConfig config =
        SessionConfig.builder()
            .user(existingUser)
            .timeLimitMinutes(30)
            .postLimit(100)
            .allowSnooze(false)
            .build();
    sessionConfigRepository.save(config);

    SessionHistory history =
        SessionHistory.builder()
            .user(existingUser)
            .start(LocalDateTime.now().minusMinutes(5))
            .seconds(300L) // 5 Minuten verbracht
            .postsViewed(100)
            .build();
    sessionRepository.save(history);

    mockMvc
        .perform(
            post(sessionEndpoint + "/heartbeat")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  public void snooze_shouldReturnOk_EvenWhenPostLimitIsExceeded() throws Exception {
    SessionConfig config =
        SessionConfig.builder()
            .user(existingUser)
            .timeLimitMinutes(30)
            .postLimit(100)
            .allowSnooze(true)
            .build();
    sessionConfigRepository.save(config);

    SessionHistory history =
        SessionHistory.builder()
            .user(existingUser)
            .start(LocalDateTime.now().minusMinutes(5))
            .seconds(300L)
            .postsViewed(100)
            .build();
    sessionRepository.save(history);

    mockMvc
        .perform(
            post(sessionEndpoint + "/snooze")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk());

    SessionConfig dbConfig =
        sessionConfigRepository.findByUserId(existingUser.getId()).orElseThrow();
    assertNotNull(dbConfig.getLastSnooze());
  }
}

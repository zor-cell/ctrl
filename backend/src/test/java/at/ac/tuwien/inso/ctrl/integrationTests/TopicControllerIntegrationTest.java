package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.topic.dto.TopicCreateRequest;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.topic.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.user.dto.Role;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class TopicControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private TopicRepository topicRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  @MockitoBean private EmbeddingService embeddingService;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private String regularUserJwtToken;
  private String adminJwtToken;
  private Topic existingTopic;

  private final String topicEndpoint = "/api/topics";
  private final float[] dummyEmbedding = new float[768];

  @BeforeEach
  public void setUp() {
    userRepository.deleteAll();

    User regularUser =
        User.builder()
            .username("regularUser")
            .password(passwordEncoder.encode("securePass123!"))
            .name("Regular User")
            .email("user@test.com")
            .role(Role.USER)
            .emailVerified(true)
            .build();
    userRepository.save(regularUser);
    regularUserJwtToken = jwtService.generateToken(regularUser);

    User adminUser =
        User.builder()
            .username("adminUser")
            .password(passwordEncoder.encode("securePass123!"))
            .name("Admin User")
            .email("admin@test.com")
            .role(Role.ADMIN)
            .emailVerified(true)
            .build();
    userRepository.save(adminUser);
    adminJwtToken = jwtService.generateToken(adminUser);

    existingTopic = new Topic();
    existingTopic.setName("Artificial Intelligence");
    existingTopic.setEmoji("🤖");
    existingTopic.setEmbedding(dummyEmbedding);
    existingTopic = topicRepository.save(existingTopic);
  }

  @Test
  public void getAvailableTopics_ShouldBeAllowedForRegularUsers() throws Exception {
    mockMvc
        .perform(
            get(topicEndpoint).header(HttpHeaders.AUTHORIZATION, "Bearer " + regularUserJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Artificial Intelligence"));
  }

  @Test
  public void createTopic_ShouldSucceed_WhenUserIsAdmin() throws Exception {
    TopicCreateRequest request =
        new TopicCreateRequest("Software Engineering", "💻", "ASE Context");
    when(embeddingService.embedText(anyString())).thenReturn(dummyEmbedding);

    mockMvc
        .perform(
            post(topicEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Software Engineering"));

    assertTrue(topicRepository.existsByName("Software Engineering"));
  }

  @Test
  public void createTopic_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
    TopicCreateRequest request = new TopicCreateRequest("Hacking", "💀", "Malicious Topic");

    mockMvc
        .perform(
            post(topicEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + regularUserJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  public void updateTopic_ShouldSucceed_WhenUserIsAdmin() throws Exception {
    TopicCreateRequest request = new TopicCreateRequest("Deep Learning", "🧠", "New Description");
    when(embeddingService.embedText(anyString())).thenReturn(dummyEmbedding);

    mockMvc
        .perform(
            put(topicEndpoint + "/" + existingTopic.getTopicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    Topic dbTopic = topicRepository.findById(existingTopic.getTopicId()).orElseThrow();
    assertEquals("Deep Learning", dbTopic.getName());
  }

  @Test
  public void updateTopic_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
    TopicCreateRequest request = new TopicCreateRequest("Defaced Name", "💥", "Attack");

    mockMvc
        .perform(
            put(topicEndpoint + "/" + existingTopic.getTopicId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + regularUserJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}

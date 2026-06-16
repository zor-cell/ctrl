package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.algorithm.dto.AlgorithmConfigDto;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.topic.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class AlgorithmControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;
    @Autowired private AlgorithmRepository algorithmRepository;
    @Autowired private TopicRepository topicRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User existingUser;
    private String validJwtToken;
    private Topic testTopic;

    private final String algorithmEndpoint = "/api/algorithm";

    @BeforeEach
    public void setUp() {
        algorithmRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();

        existingUser =
                User.builder()
                        .username("algorithmTestUser")
                        .password(passwordEncoder.encode("securePass123!"))
                        .name("Algorithm Test")
                        .email("algorithm@test.com")
                        .emailVerified(true)
                        .build();

        userRepository.save(existingUser);
        validJwtToken = jwtService.generateToken(existingUser);

        testTopic = new Topic();
        testTopic.setName("Technology");
        testTopic.setEmoji("💻");
        topicRepository.save(testTopic);
    }

    @AfterEach
    public void tearDown() {
        algorithmRepository.deleteAll();
        topicRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void getConfig_shouldReturnDefaultConfig_WhenNoConfigExists() throws Exception {
        mockMvc
                .perform(
                        get(algorithmEndpoint + "/config")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightInteraction").value(70))
                .andExpect(jsonPath("$.weightRecency").value(60))
                .andExpect(jsonPath("$.weightExploration").value(70))
                .andExpect(jsonPath("$.weightSourceQuality").value(50))
                .andExpect(jsonPath("$.weightPopularity").value(50))
                .andExpect(jsonPath("$.weightTopic").value(40))
                .andExpect(jsonPath("$.topics").isEmpty());

        assertEquals(1, algorithmRepository.count());
    }

    @Test
    public void updateConfig_shouldUpdateWeightsAndTopics() throws Exception {
        AlgorithmConfig config = AlgorithmConfig.builder()
                .user(existingUser)
                .weightInteraction(70)
                .weightExploration(70)
                .topics(new HashSet<>())
                .build();
        algorithmRepository.save(config);

        var newTopics = List.of(testTopic.getTopicId());
        AlgorithmConfigDto request = new AlgorithmConfigDto(
                85, 40, 90, 60, 30, 80, newTopics
        );

        mockMvc
                .perform(
                        put(algorithmEndpoint + "/config")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightInteraction").value(85))
                .andExpect(jsonPath("$.weightExploration").value(90))
                .andExpect(jsonPath("$.topics[0]").value(testTopic.getTopicId()));

        AlgorithmConfig dbConfig = algorithmRepository.findByUserId(existingUser.getId()).orElseThrow();
        assertEquals(85, dbConfig.getWeightInteraction());
        assertEquals(90, dbConfig.getWeightExploration());
        assertEquals(1, dbConfig.getTopics().size());
    }
}
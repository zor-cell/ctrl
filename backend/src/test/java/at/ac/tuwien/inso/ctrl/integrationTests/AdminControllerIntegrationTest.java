package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.dto.BusinessVerificationDecisionRequest;
import at.ac.tuwien.inso.ctrl.user.dto.Role;
import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerification;
import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerificationStatus;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.BusinessVerificationRepository;
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
public class AdminControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private BusinessVerificationRepository businessVerificationRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private User standardUser;
  private User adminUser;
  private BusinessVerification standardBusinessVerification;
  private String userJwtToken;
  private String adminJwtToken;

  private final String adminEndpoint = "/api/admin";

  @BeforeEach
  public void setUp() {
    userRepository.deleteAll();

    standardUser =
        User.builder()
            .username("standardUser")
            .password(passwordEncoder.encode("pass"))
            .email("user@test.com")
            .role(Role.USER)
            .isVerified(false)
            .build();
    userRepository.save(standardUser);
    userJwtToken = jwtService.generateToken(standardUser);

    adminUser =
        User.builder()
            .username("adminUser")
            .password(passwordEncoder.encode("pass"))
            .email("admin@test.com")
            .role(Role.ADMIN)
            .isVerified(true)
            .build();
    userRepository.save(adminUser);
    adminJwtToken = jwtService.generateToken(adminUser);

    standardBusinessVerification =
        BusinessVerification.builder()
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(standardUser)
            .build();
    standardBusinessVerification =
        businessVerificationRepository.save(standardBusinessVerification);
  }

  @AfterEach
  public void tearDown() {
    businessVerificationRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  public void deleteRequest_withValidUsername_shouldSuspendUser() throws Exception {
    mockMvc
        .perform(
            delete(adminEndpoint + "/" + standardUser.getUsername())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk());


    User updatedUser = userRepository.findById(standardUser.getId()).orElseThrow();
    assertTrue(updatedUser.isSuspended());
  }

  @Test
  public void getPendingRequests_shouldReturnForbidden_WhenCalledByStandardUser() throws Exception {
    mockMvc
        .perform(
            get(adminEndpoint + "/verifications/pending")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken))
        .andExpect(status().isForbidden());
  }

  @Test
  public void getPendingRequests_shouldReturnRequests_WhenCalledByAdmin() throws Exception {
    mockMvc
        .perform(
            get(adminEndpoint + "/verifications/pending")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk());
  }

  @Test
  public void getPendingRequests_shouldReturnListWithOneEntry_WhenCalledByAdmin() throws Exception {
    mockMvc
        .perform(
            get(adminEndpoint + "/verifications/pending")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  public void reviewRequests_shouldPersistChanges_WhenGivenValidVerification() throws Exception {
    BusinessVerificationDecisionRequest decisionRequest =
        new BusinessVerificationDecisionRequest(BusinessVerificationStatus.APPROVED);

    mockMvc
        .perform(
            put(adminEndpoint
                    + "/verifications/"
                    + standardBusinessVerification.getId()
                    + "/decision")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decisionRequest)))
        .andExpect(status().isOk());

    BusinessVerification updatedVerification =
        businessVerificationRepository.findById(standardBusinessVerification.getId()).orElseThrow();

    User updatedUser = userRepository.findById(standardUser.getId()).orElseThrow();

    assertEquals(BusinessVerificationStatus.APPROVED, updatedVerification.getStatus());
    assertTrue(updatedUser.isVerified());
  }

  @Test
  public void reviewRequests_shouldPersistChanges_WhenGivenRejectedVerification() throws Exception {
    BusinessVerificationDecisionRequest decisionRequest =
        new BusinessVerificationDecisionRequest(BusinessVerificationStatus.REJECTED);

    mockMvc
        .perform(
            put(adminEndpoint
                    + "/verifications/"
                    + standardBusinessVerification.getId()
                    + "/decision")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decisionRequest)))
        .andExpect(status().isOk());

    BusinessVerification updatedVerification =
        businessVerificationRepository.findById(standardBusinessVerification.getId()).orElseThrow();

    User updatedUser = userRepository.findById(standardUser.getId()).orElseThrow();

    assertEquals(BusinessVerificationStatus.REJECTED, updatedVerification.getStatus());
    assertFalse(updatedUser.isVerified());
  }
}

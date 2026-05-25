package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.dto.RelationshipStatusItem;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class UserSearchIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Autowired private FollowRelationRepository followRelationRepository;

  @Autowired private FriendRelationRepository friendRelationRepository;

  @Autowired private FriendRequestRepository friendRequestRepository;

  @Autowired private UserRepository userRepository;

  private MockMvc mockMvc;

  private User currentUser;
  private User bert;
  private User dagobert;
  private User bart;
  private User bertram;

  @BeforeEach
  public void setup() {
    mockMvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    currentUser =
        User.builder()
            .username("currentuser")
            .email("current@test.com")
            .password("password123")
            .name("Current User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    currentUser = userRepository.save(currentUser);

    bert =
        User.builder()
            .username("bert")
            .email("bert@test.com")
            .password("password123")
            .name("Bert")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    bert = userRepository.save(bert);

    dagobert =
        User.builder()
            .username("dagobert")
            .email("dagobert@test.com")
            .password("password123")
            .name("Dagobert Duck")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    dagobert = userRepository.save(dagobert);

    bart =
        User.builder()
            .username("bart")
            .email("bart@test.com")
            .password("password123")
            .name("Bart")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    bart = userRepository.save(bart);

    bertram =
        User.builder()
            .username("bertram")
            .email("bertram@test.com")
            .password("password123")
            .name("Bertram")
            .isVerified(false)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    bertram = userRepository.save(bertram);
  }

  @AfterEach
  public void tearDownTestData() {
    friendRequestRepository.deleteAll();
    friendRelationRepository.deleteAll();
    followRelationRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  public void searchUsers_basicSearch_shouldReturnMatching() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bertram")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("currentuser")));
  }

  @Test
  public void searchUsers_caseInsensitive_shouldWork() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"BERT\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bertram")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("currentuser")));
  }

  @Test
  public void searchUsers_shouldExcludeSelf() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"current\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertFalse(responses.stream().anyMatch(r -> r.username().equals("currentuser")));
  }

  @Test
  public void searchUsers_emptyQuery_shouldReturnBadRequest() throws Exception {
    mockMvc
        .perform(post("/api/search/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"query\":\"\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
            .with(user(currentUser)))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void searchUsers_noMatches_shouldReturnEmptyList() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"ghost123\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(responses.isEmpty());
  }

  // Filter tests

  @Test
  public void searchUsers_excludeFriends_shouldFilterFriends() throws Exception {
    createFriendship(currentUser, bert);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bert\",\"excludeFriends\":true,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bert")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bertram")));
  }

  // Filter tests

  @Test
  public void searchUsers_excludeFollowed_shouldFilterFollowed() throws Exception {
    createFollow(currentUser, dagobert);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":true,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bertram")));
  }

  @Test
  public void searchUsers_verifiedOnly_shouldFilterUnverified() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":true}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(2, responses.size());
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bertram")));
  }

  @Test
  public void searchUsers_combinedFilters_shouldWorkTogether() throws Exception {
    createFriendship(currentUser, dagobert);
    createFollow(currentUser, bart);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bert\",\"excludeFriends\":true,\"excludeFollowed\":true,\"verifiedOnly\":true}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("bertram")));
  }

  // Relationship status

  @Test
  public void searchUsers_statusFriend_shouldBeCorrect() throws Exception {
    createFriendship(currentUser, dagobert);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"dago\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertTrue(hasStatus(responses.get(0), RelationshipStatusItem.RelationshipStatus.FRIEND));
  }

  @Test
  public void searchUsers_statusFollowed_shouldBeCorrect() throws Exception {
    createFollow(currentUser, bertram);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"ram\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertTrue(hasStatus(responses.get(0), RelationshipStatusItem.RelationshipStatus.FOLLOWED));
  }

  @Test
  public void searchUsers_statusPendingSent_shouldBeCorrect() throws Exception {
    createFriendRequest(currentUser, bart);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bart\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertTrue(
        hasStatus(
            responses.get(0), RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_SENT));
  }

  @Test
  public void searchUsers_statusPendingIncoming_shouldBeCorrect() throws Exception {
    createFriendRequest(dagobert, currentUser);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"dag\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertTrue(
        hasStatus(
            responses.get(0), RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_INCOMING));
  }

  @Test
  public void searchUsers_statusNone_shouldBeCorrect() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(3, responses.size());
    assertTrue(responses.stream().allMatch(r -> r.relationshipStatuses().isEmpty()));
  }

  // Display name tests

  @Test
  public void searchUsers_displayName_verifiedFriend_shouldShowRealName() throws Exception {
    createFriendship(currentUser, dagobert);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"dagobert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertEquals("Dagobert Duck", responses.get(0).name());
  }

  @Test
  public void searchUsers_displayName_unverifiedNonFriend_shouldBeNull() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bertram\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertNull(responses.get(0).name());
  }

  @Test
  public void searchUsers_displayName_verifiedNonFriend_shouldShowReal() throws Exception {
    createFollow(currentUser, dagobert);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"obert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertEquals("Dagobert Duck", responses.get(0).name());
  }

  @Test
  public void searchUsers_displayName_unverifiedFriend_shouldShowReal() throws Exception {
    createFriendship(currentUser, bertram);

    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bertr\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertEquals("Bertram", responses.get(0).name());
  }

  @Test
  public void searchUsers_returnsCorrectFields() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/search/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"bart\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                .with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<UserSearchResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    UserSearchResponse response = responses.get(0);

    assertEquals(bart.getId(), response.id());
    assertEquals("bart", response.username());
    assertEquals("Bart", response.name());
    assertTrue(response.isVerified());
    assertTrue(response.relationshipStatuses().isEmpty());
  }

  // Helper methods

  private void createFollow(User follower, User followed) {
    FollowRelation relation =
        FollowRelation.builder()
            .id(new FollowRelationId(follower.getId(), followed.getId()))
            .follower(follower)
            .followed(followed)
            .since(LocalDateTime.now())
            .build();
    followRelationRepository.save(relation);
  }

  private void createMutualFollows(User user1, User user2) {
    createFollow(user1, user2);
    createFollow(user2, user1);
  }

  private void createFriendRequest(User sender, User receiver) {
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(sender.getId(), receiver.getId()))
            .sender(sender)
            .receiver(receiver)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(30))
            .build();
    friendRequestRepository.save(request);
  }

  private void createFriendship(User user1, User user2) {
    FriendRelation friendship =
        FriendRelation.builder()
            .id(new FriendRelationId(user1.getId(), user2.getId()))
            .friend1(user1)
            .friend2(user2)
            .since(LocalDateTime.now())
            .build();
    friendRelationRepository.save(friendship);
  }

  private boolean hasStatus(
      UserSearchResponse response, RelationshipStatusItem.RelationshipStatus status) {
    return response.relationshipStatuses().stream().anyMatch(item -> item.status() == status);
  }
}

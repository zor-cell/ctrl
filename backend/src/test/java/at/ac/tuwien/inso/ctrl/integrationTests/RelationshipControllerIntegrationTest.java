package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.chat.ChatService;
import at.ac.tuwien.inso.ctrl.user.dto.BlockedResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FollowedResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestAction;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelation;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.BlockedRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.BlockService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
public class RelationshipControllerIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private UserRepository userRepository;
  @Autowired private BlockService blockService;
  @Autowired private BlockedRelationRepository blockedRelationRepository;
  @Autowired private FollowRelationRepository followRelationRepository;
  @Autowired private FriendRelationRepository friendRelationRepository;
  @Autowired private FriendRequestRepository friendRequestRepository;
  @MockitoBean private ChatService chatService;
  @PersistenceContext private EntityManager entityManager;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private MockMvc mockMvc;

  private User currentUser;
  private User regularUser;
  private User suspendedUser;

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
            .isSuspended(false)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    currentUser = userRepository.save(currentUser);

    regularUser =
        User.builder()
            .username("regularuser")
            .email("regular@test.com")
            .password("password123")
            .name("Regular User")
            .isVerified(true)
            .emailVerified(true)
            .isSuspended(false)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    regularUser = userRepository.save(regularUser);

    suspendedUser =
        User.builder()
            .username("suspendeduser")
            .email("suspended@test.com")
            .password("password123")
            .name("Suspended User")
            .isVerified(true)
            .emailVerified(true)
            .isSuspended(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    suspendedUser = userRepository.save(suspendedUser);
  }

  @AfterEach
  public void tearDown() {
    friendRequestRepository.deleteAll();
    friendRelationRepository.deleteAll();
    followRelationRepository.deleteAll();
    blockedRelationRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ==================== SUSPENDED USER TESTS ====================

  @Test
  public void getFriends_excludesSuspendedUsers() throws Exception {
    // Given: Current user is friends with both a regular user and a suspended user
    createFriendship(currentUser, regularUser);
    createFriendship(currentUser, suspendedUser);

    // When: Getting friends list
    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friends").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    // Then: Suspended user should be excluded
    List<FriendResponse> friends =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(
        friends.stream().anyMatch(f -> f.username().equals("regularuser")),
        "Regular user should appear in friends list");
    assertFalse(
        friends.stream().anyMatch(f -> f.username().equals("suspendeduser")),
        "Suspended user should be excluded from friends list");
  }

  @Test
  public void getFollowed_excludesSuspendedUsers() throws Exception {
    // Given: Current user follows both a regular user and a suspended user
    createFollow(currentUser, regularUser);
    createFollow(currentUser, suspendedUser);

    // When: Getting followed list
    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/followed").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    // Then: Suspended user should be excluded
    List<FollowedResponse> followed =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(
        followed.stream().anyMatch(f -> f.username().equals("regularuser")),
        "Regular user should appear in followed list");
    assertFalse(
        followed.stream().anyMatch(f -> f.username().equals("suspendeduser")),
        "Suspended user should be excluded from followed list");
  }

  @Test
  public void sendFriendRequest_toSuspendedUser_returnsNotFound() throws Exception {
    // Given: A suspended user
    String requestBody = String.format("{\"toUsername\":\"%s\"}", suspendedUser.getUsername());

    // When/Then: Sending friend request to suspended user should return 404 (Not Found)
    mockMvc
        .perform(
            post("/api/users/me/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  public void followSuspendedUser_returnsNotFound() throws Exception {
    // Given: A suspended user

    // When/Then: Following a suspended user should return 404 (Not Found)
    mockMvc
        .perform(
            post("/api/users/me/followed/{username}", suspendedUser.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  public void getSentFriendRequests_excludesSuspendedUsers() throws Exception {
    // Given: Current user has sent friend requests to both regular and suspended users
    createFriendRequest(currentUser, regularUser);
    createFriendRequest(currentUser, suspendedUser);

    // When: Getting sent friend requests
    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friend-requests/sent").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    // Then: Requests to suspended users should be excluded
    List<FriendRequestResponse> requests =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(
        requests.stream().anyMatch(r -> r.username().equals("regularuser")),
        "Request to regular user should appear");
    assertFalse(
        requests.stream().anyMatch(r -> r.username().equals("suspendeduser")),
        "Request to suspended user should be excluded");
  }

  @Test
  public void getIncomingFriendRequests_excludesSuspendedUsers() throws Exception {
    // Given: Current user has incoming friend requests from both regular and suspended users
    createFriendRequest(regularUser, currentUser);
    createFriendRequest(suspendedUser, currentUser);

    // When: Getting incoming friend requests
    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friend-requests/incoming").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    // Then: Requests from suspended users should be excluded
    List<FriendRequestResponse> requests =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(
        requests.stream().anyMatch(r -> r.username().equals("regularuser")),
        "Request from regular user should appear");
    assertFalse(
        requests.stream().anyMatch(r -> r.username().equals("suspendeduser")),
        "Request from suspended user should be excluded");
  }

  @Test
  public void acceptFriendRequest_fromSuspendedUser_returnsNotFound() throws Exception {
    // Given: A suspended user sent a friend request to current user
    createFriendRequest(suspendedUser, currentUser);

    String requestBody = String.format("{\"action\":\"%s\"}", FriendRequestAction.ACCEPT);

    // When/Then: Accepting request from suspended user should return 404
    mockMvc
        .perform(
            patch("/api/users/me/friend-requests/{username}", suspendedUser.getUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isNotFound());
  }

  // block tests

  @Test
  public void block_withValidUser_shouldCreateBlockedRelationAndPublishChatRemoval()
      throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/block/{username}", regularUser.getUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .with(user(currentUser)))
        .andExpect(status().isCreated());

    assertTrue(
        blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(
            currentUser, regularUser));
    verify(chatService).publishBlockedDirectChatRemoval(currentUser, regularUser.getUsername());
  }

  @Test
  public void unblock_withExistingBlockedRelation_shouldDeleteRelation() throws Exception {
    createBlockedRelation(currentUser, regularUser);

    mockMvc
        .perform(
            delete("/api/users/me/block/{username}", regularUser.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isNoContent());

    assertFalse(
        blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(
            currentUser, regularUser));
  }

  @Test
  public void unblock_withNoBlockedRelation_shouldReturnNotFound() throws Exception {
    mockMvc
        .perform(
            delete("/api/users/me/block/{username}", regularUser.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  public void getBlockedUsers_withoutSearch_shouldExcludeSuspendedUsers() throws Exception {
    createBlockedRelation(currentUser, regularUser);
    createBlockedRelation(currentUser, suspendedUser);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/block").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<BlockedResponse> blockedUsers =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, blockedUsers.size());
    assertEquals(regularUser.getUsername(), blockedUsers.get(0).username());
  }

  @Test
  public void getBlockedUsers_withSearch_shouldReturnOnlyMatchingUsers() throws Exception {
    User anotherUser =
        userRepository.save(
            User.builder()
                .username("anotheruser")
                .email("another@test.com")
                .password("password123")
                .name("Another User")
                .isVerified(true)
                .emailVerified(true)
                .isSuspended(false)
                .birthdate(LocalDate.of(2000, 1, 1))
                .build());
    createBlockedRelation(currentUser, regularUser);
    createBlockedRelation(currentUser, anotherUser);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/block").param("q", "regular").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<BlockedResponse> blockedUsers =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, blockedUsers.size());
    assertEquals(regularUser.getUsername(), blockedUsers.get(0).username());
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

  private void createBlockedRelation(User user, User blockedUser) {
    BlockedRelation relation =
        BlockedRelation.builder()
            .id(new BlockedRelationId(user.getId(), blockedUser.getId()))
            .user(user)
            .blockedUser(blockedUser)
            .since(LocalDateTime.now())
            .build();
    blockedRelationRepository.save(relation);
  }
}

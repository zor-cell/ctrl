package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.dto.FollowedResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
public class RelationshipControllerIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private FollowRelationRepository followRelationRepository;

  @Autowired private FriendRequestRepository friendRequestRepository;

  @Autowired private UserRepository userRepository;

  @PersistenceContext private EntityManager entityManager;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private MockMvc mockMvc;

  protected User currentUser;
  protected User alpha;
  protected User beta;
  protected User gamma;
  protected User delta;

  @BeforeEach
  public void setupTestData() {
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

    alpha =
        User.builder()
            .username("alpha")
            .email("alpha@test.com")
            .password("password123")
            .name("Alpha")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    alpha = userRepository.save(alpha);

    beta =
        User.builder()
            .username("beta")
            .email("beta@test.com")
            .password("password123")
            .name("Beta")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    beta = userRepository.save(beta);

    gamma =
        User.builder()
            .username("gamma")
            .email("gamma@test.com")
            .password("password123")
            .name("Gamma")
            .isVerified(false)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    gamma = userRepository.save(gamma);

    delta =
        User.builder()
            .username("delta")
            .email("delta@test.com")
            .password("password123")
            .name("Delta")
            .isVerified(false)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
    delta = userRepository.save(delta);
  }

  @AfterEach
  public void tearDownTestData() {
    friendRequestRepository.deleteAll();
    followRelationRepository.deleteAll();
    userRepository.deleteAll();
  }

  // GET /users/me/friends

  @Test
  public void getFriends_withoutSearch_shouldReturnAllFriends() throws Exception {
    createMutualFollows(currentUser, alpha);
    createMutualFollows(currentUser, gamma);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friends").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<FriendResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(2, responses.size());
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("alpha")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("beta")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("gamma")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("delta")));
  }

  @Test
  public void getFriends_withSearch_shouldReturnFriendsContainingSearchSubstring()
      throws Exception {
    createMutualFollows(currentUser, alpha);
    createMutualFollows(currentUser, beta);
    createMutualFollows(currentUser, gamma);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friends").param("q", "pha").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<FriendResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("alpha")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("beta")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("gamma")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("delta")));
  }

  @Test
  public void getFriends_noFriends_shouldReturnEmptyList() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/api/users/me/friends").with(user(currentUser))).andReturn();

    List<FriendResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(responses.isEmpty());
  }

  // GET /users/me/followed

  @Test
  public void getFollowed_shouldReturnAllFollowed() throws Exception {
    createFollow(currentUser, alpha);
    createFollow(currentUser, beta);
    createMutualFollows(currentUser, gamma);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/followed").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<FollowedResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(3, responses.size());
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("alpha")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("beta")));
    assertTrue(responses.stream().anyMatch(r -> r.username().equals("gamma")));
    assertFalse(responses.stream().anyMatch(r -> r.username().equals("delta")));
  }

  @Test
  public void getFollowed_withSearch_shouldReturnFollowedContainingSearchSubstring()
      throws Exception {
    createFollow(currentUser, alpha);
    createFollow(currentUser, beta);
    createFollow(currentUser, gamma);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/followed").param("q", "pha").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<FollowedResponse> responses =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, responses.size());
    assertTrue(responses.getFirst().username().equals("alpha"));
  }

  // POST /users/me/followed/{username}

  @Test
  public void follow_verifiedUser_shouldCreateFollowRelation() throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/followed/{username}", alpha.getUsername()).with(user(currentUser)))
        .andExpect(status().isCreated());

    // Verify follow relation was created
    Optional<FollowRelation> relation =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertTrue(relation.isPresent());
  }

  @Test
  public void follow_unverifiedUser_shouldFail() throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/followed/{username}", gamma.getUsername()).with(user(currentUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Can only follow verified users"));

    // Verify no follow relation was created
    Optional<FollowRelation> relation =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), gamma.getId()));
    assertFalse(relation.isPresent());
  }

  @Test
  public void follow_self_shouldFail() throws Exception {
    mockMvc
        .perform(
            post("/api/users/me/followed/{username}", currentUser.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Cannot follow yourself"));
  }

  @Test
  public void follow_nonExistentUser_shouldFail() throws Exception {
    mockMvc
        .perform(post("/api/users/me/followed/{username}", "ghost123").with(user(currentUser)))
        .andExpect(status().isNotFound())
        .andExpect(content().string("User with identifier 'ghost123' not found."));
  }

  @Test
  public void follow_alreadyFollowing_shouldSucceedIdempotent() throws Exception {
    createFollow(currentUser, alpha);

    mockMvc
        .perform(
            post("/api/users/me/followed/{username}", alpha.getUsername()).with(user(currentUser)))
        .andExpect(status().isCreated());

    long count =
        followRelationRepository.findFollowedByFollower(currentUser).stream()
            .filter(u -> u.getId().equals(alpha.getId()))
            .count();
    assertEquals(1, count);
  }

  // DELETE /users/me/followed/{username} tests

  @Test
  public void unfollow_existing_shouldRemoveRelation() throws Exception {
    createFollow(currentUser, alpha);

    mockMvc
        .perform(
            delete("/api/users/me/followed/{username}", alpha.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isNoContent());

    entityManager.flush();
    entityManager.clear();

    Optional<FollowRelation> deleted =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertFalse(deleted.isPresent());
  }

  @Test
  public void unfollow_breaksFriendshipBothWays() throws Exception {
    createMutualFollows(currentUser, alpha);

    mockMvc
        .perform(
            delete("/api/users/me/followed/{username}", alpha.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isNoContent());

    entityManager.flush();
    entityManager.clear();

    Optional<FollowRelation> currentToFriend =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    Optional<FollowRelation> friendToCurrent =
        followRelationRepository.findById(new FollowRelationId(alpha.getId(), currentUser.getId()));

    assertFalse(currentToFriend.isPresent());
    assertFalse(friendToCurrent.isPresent());
  }

  @Test
  public void unfollow_nonExistentUser_shouldFail() throws Exception {
    mockMvc
        .perform(delete("/api/users/me/followed/{username}", "ghost123").with(user(currentUser)))
        .andExpect(status().isNotFound())
        .andExpect(content().string("User with identifier 'ghost123' not found."));
  }

  // GET /users/me/friend-requests/sent

  @Test
  public void getSentFriendRequests_shouldOnlyReturnSentRequests() throws Exception {
    createFriendRequest(currentUser, alpha);
    createFriendRequest(beta, currentUser);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friend-requests/sent").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<FriendRequestResponse> response =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, response.size());
    assertTrue(response.getFirst().username().equals("alpha"));
    assertFalse(response.getFirst().isIncoming());
  }

  @Test
  public void getSentFriendRequests_withExpired_shouldFilterAndDelete() throws Exception {
    createFriendRequest(currentUser, alpha);

    FriendRequest expiredRequest =
        FriendRequest.builder()
            .id(new FriendRequestId(currentUser.getId(), alpha.getId()))
            .sender(currentUser)
            .receiver(alpha)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(expiredRequest);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friend-requests/sent").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<FriendRequestResponse> response =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(response.isEmpty());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertFalse(deleted.isPresent());
  }

  // GET /users/me/friend-requests/incoming

  @Test
  public void getIncomingFriendRequests_shouldOnlyReturnIncomingRequests() throws Exception {
    createFriendRequest(alpha, currentUser);
    createFriendRequest(currentUser, beta);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friend-requests/incoming").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<FriendRequestResponse> response =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertEquals(1, response.size());
    assertTrue(response.getFirst().username().equals("alpha"));
    assertTrue(response.getFirst().isIncoming());
  }

  @Test
  public void getIncomingFriendRequests_withExpired_shouldFilterAndDelete() throws Exception {
    createFriendRequest(alpha, currentUser);

    FriendRequest expiredRequest =
        FriendRequest.builder()
            .id(new FriendRequestId(alpha.getId(), currentUser.getId()))
            .sender(alpha)
            .receiver(currentUser)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(expiredRequest);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friend-requests/incoming").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    List<FriendRequestResponse> response =
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

    assertTrue(response.isEmpty());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(alpha.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent());
  }

  // POST /users/me/friend-requests

  @Test
  public void sendFriendRequest_toVerified_shouldCreateRequestAndFollow() throws Exception {
    String requestBody = "{\"toUsername\": \"" + alpha.getUsername() + "\"}";

    mockMvc
        .perform(
            post("/api/users/me/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isCreated());

    Optional<FriendRequest> request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertTrue(request.isPresent());

    Optional<FollowRelation> follow =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertTrue(follow.isPresent());
  }

  @Test
  public void sendFriendRequest_toUnverified_shouldCreateRequestOnly() throws Exception {
    String requestBody = "{\"toUsername\": \"" + gamma.getUsername() + "\"}";

    mockMvc
        .perform(
            post("/api/users/me/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isCreated());

    Optional<FriendRequest> request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), gamma.getId()));
    assertTrue(request.isPresent());

    Optional<FollowRelation> follow =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), gamma.getId()));
    assertFalse(follow.isPresent());
  }

  @Test
  public void sendFriendRequest_toSelf_shouldFail() throws Exception {
    String requestBody = "{\"toUsername\": \"" + currentUser.getUsername() + "\"}";

    mockMvc
        .perform(
            post("/api/users/me/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Cannot send friend request to yourself"));
  }

  @Test
  public void sendFriendRequest_alreadyFriends_shouldFail() throws Exception {
    createMutualFollows(currentUser, alpha);

    String requestBody = "{\"toUsername\": \"" + alpha.getUsername() + "\"}";

    mockMvc
        .perform(
            post("/api/users/me/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("You are already friends with this user"));
  }

  @Test
  public void sendFriendRequest_duplicatePending_shouldFail() throws Exception {
    createFriendRequest(currentUser, alpha);

    String requestBody = "{\"toUsername\": \"" + alpha.getUsername() + "\"}";

    mockMvc
        .perform(
            post("/api/users/me/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isConflict())
        .andExpect(
            content()
                .string(
                    "Friend request with receiver '" + alpha.getUsername() + "' already exists."));
  }

  @Test
  public void sendFriendRequest_whenTargetAlreadySent_shouldFail() throws Exception {
    createFriendRequest(alpha, currentUser);

    String requestBody = "{\"toUsername\": \"" + alpha.getUsername() + "\"}";

    mockMvc
        .perform(
            post("/api/users/me/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isBadRequest())
        .andExpect(
            content()
                .string(
                    "This user has already sent you a friend request. Please accept or decline"
                        + " it."));
  }

  // PATCH /users/me/friend-requests/{username}

  @Test
  public void handleFriendRequest_accept_shouldCreateFriendship() throws Exception {
    createFriendRequest(alpha, currentUser);

    String requestBody = "{\"action\": \"ACCEPT\"}";

    mockMvc
        .perform(
            patch("/api/users/me/friend-requests/{username}", alpha.getUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isNoContent());

    Optional<FollowRelation> receiverToSender =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    Optional<FollowRelation> senderToReceiver =
        followRelationRepository.findById(new FollowRelationId(alpha.getId(), currentUser.getId()));

    assertTrue(receiverToSender.isPresent());
    assertTrue(senderToReceiver.isPresent());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(alpha.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent());
  }

  @Test
  public void handleFriendRequest_decline_shouldDeleteRequestAndDeleteAnyFollows()
      throws Exception {
    createFriendRequest(alpha, currentUser);

    String requestBody = "{\"action\": \"DECLINE\"}";

    mockMvc
        .perform(
            patch("/api/users/me/friend-requests/{username}", alpha.getUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isNoContent());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(alpha.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent());

    Optional<FollowRelation> followRelation =
        followRelationRepository.findById(new FollowRelationId(alpha.getId(), currentUser.getId()));
    assertFalse(followRelation.isPresent());
  }

  @Test
  public void handleFriendRequest_nonExistentSender_shouldFail() throws Exception {
    String requestBody = "{\"action\": \"ACCEPT\"}";

    mockMvc
        .perform(
            patch("/api/users/me/friend-requests/{username}", "ghost123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isNotFound())
        .andExpect(content().string("User with identifier 'ghost123' not found."));
  }

  @Test
  public void handleFriendRequest_expired_shouldFail() throws Exception {
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(alpha.getId(), currentUser.getId()))
            .sender(alpha)
            .receiver(currentUser)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(request);

    String requestBody = "{\"action\": \"ACCEPT\"}";

    mockMvc
        .perform(
            patch("/api/users/me/friend-requests/{username}", alpha.getUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("This friend request has expired"));

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(alpha.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent());
  }

  @Test
  public void handleFriendRequest_invalidAction_shouldFail() throws Exception {
    createFriendRequest(alpha, currentUser);

    String requestBody = "{}";

    mockMvc
        .perform(
            patch("/api/users/me/friend-requests/{username}", alpha.getUsername())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(user(currentUser)))
        .andExpect(status().isBadRequest());
  }

  // DELETE /users/me/friend-requests/{username}

  @Test
  public void cancelFriendRequest_toUnverified_shouldSucceed() throws Exception {
    createFriendRequest(currentUser, gamma);

    mockMvc
        .perform(
            delete("/api/users/me/friend-requests/{username}", gamma.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isNoContent());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), gamma.getId()));
    assertFalse(deleted.isPresent());
  }

  @Test
  public void cancelFriendRequest_toVerified_shouldKeepFollowRelation() throws Exception {
    createFollow(currentUser, alpha);
    createFriendRequest(currentUser, alpha);

    mockMvc
        .perform(
            delete("/api/users/me/friend-requests/{username}", alpha.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isNoContent());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertFalse(deleted.isPresent());

    Optional<FollowRelation> kept =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertTrue(kept.isPresent());
  }

  @Test
  public void cancelFriendRequest_nonExistent_shouldFail() throws Exception {
    mockMvc
        .perform(
            delete("/api/users/me/friend-requests/{username}", "ghost123").with(user(currentUser)))
        .andExpect(status().isNotFound())
        .andExpect(content().string("User with identifier 'ghost123' not found."));
  }

  @Test
  public void cancelFriendRequest_expired_shouldFail() throws Exception {
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(currentUser.getId(), alpha.getId()))
            .sender(currentUser)
            .receiver(alpha)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(request);

    mockMvc
        .perform(
            delete("/api/users/me/friend-requests/{username}", alpha.getUsername())
                .with(user(currentUser)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("This friend request has expired"));

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertFalse(deleted.isPresent());
  }

  // GET /users/me/friend-requests/count

  @Test
  public void getPendingRequestCount_shouldReturnCorrectCount() throws Exception {
    createFriendRequest(alpha, currentUser);
    createFriendRequest(beta, currentUser);
    createFriendRequest(gamma, currentUser);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friend-requests/count").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    assertEquals("3", content);
  }

  @Test
  public void getPendingRequestCount_withExpired_shouldFilter() throws Exception {
    createFriendRequest(alpha, currentUser);
    createFriendRequest(beta, currentUser);

    FriendRequest expiredRequest =
        FriendRequest.builder()
            .id(new FriendRequestId(gamma.getId(), currentUser.getId()))
            .sender(gamma)
            .receiver(currentUser)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(expiredRequest);

    MvcResult result =
        mockMvc
            .perform(get("/api/users/me/friend-requests/count").with(user(currentUser)))
            .andExpect(status().isOk())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    assertEquals("2", content);

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(gamma.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent());
  }

  // Helper methods

  private void createFollow(
      at.ac.tuwien.inso.ctrl.user.entity.User follower,
      at.ac.tuwien.inso.ctrl.user.entity.User followed) {
    FollowRelation relation =
        FollowRelation.builder()
            .id(new FollowRelationId(follower.getId(), followed.getId()))
            .follower(follower)
            .followed(followed)
            .since(LocalDateTime.now())
            .build();
    followRelationRepository.save(relation);
  }

  private void createMutualFollows(
      at.ac.tuwien.inso.ctrl.user.entity.User user1,
      at.ac.tuwien.inso.ctrl.user.entity.User user2) {
    createFollow(user1, user2);
    createFollow(user2, user1);
  }

  private void createFriendRequest(
      at.ac.tuwien.inso.ctrl.user.entity.User sender,
      at.ac.tuwien.inso.ctrl.user.entity.User receiver) {
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
}

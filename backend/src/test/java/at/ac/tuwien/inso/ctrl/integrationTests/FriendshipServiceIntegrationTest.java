package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestAction;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
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
import at.ac.tuwien.inso.ctrl.user.service.FriendshipService;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class FriendshipServiceIntegrationTest {

  @Autowired private FriendshipService friendshipService;

  @Autowired private FollowRelationRepository followRelationRepository;

  @Autowired private FriendRelationRepository friendRelationRepository;

  @Autowired private FriendRequestRepository friendRequestRepository;

  @Autowired private UserRepository userRepository;

  @PersistenceContext private EntityManager entityManager;

  protected User currentUser;
  protected User alpha;
  protected User beta;
  protected User gamma;
  protected User delta;

  @BeforeEach
  public void setupTestData() {
    currentUser =
        User.builder()
            .username("currentuser")
            .email("current@test.com")
            .password("password123")
            .name("Current User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .profilePicture("users/profile-pictures/currentuser-avatar.jpg")
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
            .profilePicture("users/profile-pictures/alpha-avatar.jpg")
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
            .profilePicture(null) // No profile picture
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
            .profilePicture(null) // No profile picture
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
            .profilePicture("users/profile-pictures/delta-avatar.jpg")
            .build();
    delta = userRepository.save(delta);
  }

  @AfterEach
  public void tearDownTestData() {
    friendRequestRepository.deleteAll();
    friendRelationRepository.deleteAll();
    followRelationRepository.deleteAll();
    userRepository.deleteAll();
  }

  // getFriends()

  @Test
  public void getFriends_returnsFriendsFromFriendRelation() {
    createFriendship(currentUser, alpha);
    createFriendship(currentUser, beta);
    createFollow(currentUser, gamma);
    createFollow(delta, currentUser);

    List<FriendResponse> friends = friendshipService.getFriends(currentUser, null);

    assertEquals(2, friends.size());
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("alpha")));
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("beta")));
    assertFalse(friends.stream().anyMatch(f -> f.username().equals("gamma")));
    assertFalse(friends.stream().anyMatch(f -> f.username().equals("delta")));
  }

  @Test
  public void getFriends_withBlankQuery_returnsAllFriends() {
    createFriendship(currentUser, alpha);
    createFriendship(currentUser, beta);

    List<FriendResponse> friends = friendshipService.getFriends(currentUser, "   ");

    assertEquals(2, friends.size());
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("alpha")));
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("beta")));
  }

  @Test
  public void getFriends_doesNotReturnOneWayFollows() {
    createFollow(currentUser, alpha);
    createFollow(beta, currentUser);

    List<FriendResponse> friends = friendshipService.getFriends(currentUser, null);

    assertTrue(friends.isEmpty());
  }

  @Test
  public void getFriends_withSearchFilters_correctlyReturnsFriendsWithSearchSubstring() {
    createFriendship(currentUser, alpha);
    createFriendship(currentUser, beta);
    createFriendship(currentUser, delta);

    List<FriendResponse> friends = friendshipService.getFriends(currentUser, "ta");

    assertEquals(2, friends.size());
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("beta")));
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("delta")));
  }

  @Test
  public void getFriends_withoutFriends_returnsEmptyResult() {
    List<FriendResponse> friends = friendshipService.getFriends(currentUser, null);
    assertTrue(friends.isEmpty());
  }

  // unfriend()

  @Test
  public void unfriend_removesFriendship() {
    createFriendship(currentUser, alpha);

    friendshipService.unfriend(currentUser, alpha.getUsername());

    entityManager.flush();
    entityManager.clear();

    Optional<FriendRelation> friendship =
        friendRelationRepository.findById(new FriendRelationId(currentUser.getId(), alpha.getId()));
    assertFalse(friendship.isPresent(), "Friendship should be removed");
  }

  @Test
  public void unfriend_leavesFollowsIntact() {
    createFollow(currentUser, alpha);
    createFollow(beta, currentUser);
    createFriendship(currentUser, gamma);

    friendshipService.unfriend(currentUser, gamma.getUsername());

    entityManager.flush();
    entityManager.clear();

    Optional<FriendRelation> friendship =
        friendRelationRepository.findById(new FriendRelationId(currentUser.getId(), gamma.getId()));
    assertFalse(friendship.isPresent(), "Friendship should be removed");

    Optional<FollowRelation> currentToAlpha =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    Optional<FollowRelation> betaToAlpha =
        followRelationRepository.findById(new FollowRelationId(beta.getId(), currentUser.getId()));

    assertTrue(currentToAlpha.isPresent());
    assertTrue(betaToAlpha.isPresent());
  }

  @Test
  public void unfriend_nonExistentFriendship_noError() {
    // Should not throw any exception when trying to unfriend someone who is not a
    // friend
    assertDoesNotThrow(
        () -> friendshipService.unfriend(currentUser, alpha.getUsername()),
        "Unfriending a non-friend should not throw an error");
  }

  @Test
  public void unfriend_nonExistentUser_throwsNotFound() {
    NotFoundException exception =
        assertThrows(
            NotFoundException.class, () -> friendshipService.unfriend(currentUser, "ghost123"));

    assertEquals("User with identifier 'ghost123' not found.", exception.getMessage());
  }

  // sendRequest()
  @Test
  public void sendRequest_toVerified_createsImmediateFollow() {
    friendshipService.sendRequest(currentUser, alpha.getUsername());

    Optional<FriendRequest> request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertTrue(request.isPresent());

    Optional<FollowRelation> follow =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertTrue(follow.isPresent());
    assertEquals(currentUser.getId(), follow.get().getFollower().getId());
    assertEquals(alpha.getId(), follow.get().getFollowed().getId());
    assertNotNull(follow.get().getSince());
  }

  @Test
  public void sendRequest_toUnverified_noImmediateFollow() {
    friendshipService.sendRequest(currentUser, gamma.getUsername());

    Optional<FriendRequest> request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), gamma.getId()));
    assertTrue(request.isPresent());

    Optional<FollowRelation> follow =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), gamma.getId()));
    assertFalse(follow.isPresent());
  }

  @Test
  public void sendRequest_requestHasCorrectExpirationDate() {
    LocalDateTime beforeSend = LocalDateTime.now();
    friendshipService.sendRequest(currentUser, alpha.getUsername());
    LocalDateTime afterSend = LocalDateTime.now();

    // Verify request expiration date (should be 30 days from creation)
    Optional<FriendRequest> request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertTrue(request.isPresent());

    LocalDateTime expiresAt = request.get().getExpiresAt();
    LocalDateTime createdAt = request.get().getCreatedAt();

    // Verify expiration is 30 days after creation (compare with truncated precision
    // to avoid nanosecond issues)
    LocalDateTime expectedExpires = createdAt.plusDays(30);
    assertEquals(
        expectedExpires.truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
        expiresAt.truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
        "Expiration should be 30 days after creation");

    assertTrue(
        !createdAt.isBefore(beforeSend.minusSeconds(1))
            && !createdAt.isAfter(afterSend.plusSeconds(1)),
        "CreatedAt should be between beforeSend and afterSend");
  }

  @Test
  public void sendRequest_toSelf_shouldFail() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> friendshipService.sendRequest(currentUser, currentUser.getUsername()));

    assertEquals("Cannot send friend request to yourself", exception.getMessage());
  }

  @Test
  public void sendRequest_toNonExistent_shouldFail() {
    NotFoundException exception =
        assertThrows(
            NotFoundException.class, () -> friendshipService.sendRequest(currentUser, "ghost123"));

    assertEquals("User with identifier 'ghost123' not found.", exception.getMessage());
  }

  @Test
  public void sendRequest_alreadyFriends_shouldFail() {
    createFriendship(currentUser, alpha);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> friendshipService.sendRequest(currentUser, alpha.getUsername()));

    assertEquals("You are already friends with this user", exception.getMessage());
  }

  @Test
  public void sendRequest_duplicatePending_shouldFail() {
    createFriendRequest(currentUser, alpha);

    ResourceAlreadyExistsException exception =
        assertThrows(
            ResourceAlreadyExistsException.class,
            () -> friendshipService.sendRequest(currentUser, alpha.getUsername()));

    assertEquals(
        "Friend request with receiver '" + alpha.getUsername() + "' already exists.",
        exception.getMessage());
  }

  @Test
  public void sendRequest_whenTargetAlreadySent_shouldFail() {
    createFriendRequest(alpha, currentUser);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> friendshipService.sendRequest(currentUser, alpha.getUsername()));

    assertEquals(
        "This user has already sent you a friend request. Please accept or decline it.",
        exception.getMessage());
  }

  // acceptRequest()

  @Test
  public void acceptRequest_createsFriendship() {
    createFriendRequest(gamma, currentUser);

    friendshipService.handleIncomingRequestAction(
        currentUser, gamma.getUsername(), FriendRequestAction.ACCEPT);

    Optional<FriendRelation> friendship =
        friendRelationRepository.findById(new FriendRelationId(currentUser.getId(), gamma.getId()));

    assertTrue(friendship.isPresent(), "Friendship should be created after acceptance");

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(gamma.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent(), "Request should be deleted after acceptance");
  }

  @Test
  public void acceptRequest_whenSenderAlreadyFollows_createsFriendshipAndDeletesFollow() {
    createFollow(alpha, currentUser);

    createFriendRequest(alpha, currentUser);

    friendshipService.handleIncomingRequestAction(
        currentUser, alpha.getUsername(), FriendRequestAction.ACCEPT);

    entityManager.flush();
    entityManager.clear();

    Optional<FriendRelation> friendship =
        friendRelationRepository.findById(new FriendRelationId(currentUser.getId(), alpha.getId()));

    assertTrue(friendship.isPresent(), "Friendship should be created");

    Optional<FollowRelation> follow =
        followRelationRepository.findById(new FollowRelationId(alpha.getId(), currentUser.getId()));
    assertFalse(follow.isPresent());
  }

  @Test
  public void acceptRequest_whenReceiverAlreadyFollows_createsFriendshipAndDeletesFollow() {
    createFollow(currentUser, alpha);

    createFriendRequest(alpha, currentUser);

    friendshipService.handleIncomingRequestAction(
        currentUser, alpha.getUsername(), FriendRequestAction.ACCEPT);

    entityManager.flush();
    entityManager.clear();

    Optional<FriendRelation> friendship =
        friendRelationRepository.findById(new FriendRelationId(currentUser.getId(), alpha.getId()));

    assertTrue(friendship.isPresent(), "Friendship should be created");

    Optional<FollowRelation> follow =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertFalse(follow.isPresent());
  }

  // declineRequest()

  @Test
  public void declineRequest_doesNotCreateFriendship() {
    createFriendRequest(alpha, currentUser);

    friendshipService.handleIncomingRequestAction(
        currentUser, alpha.getUsername(), FriendRequestAction.DECLINE);

    Optional<FriendRelation> friendship =
        friendRelationRepository.findById(new FriendRelationId(currentUser.getId(), alpha.getId()));

    assertFalse(friendship.isPresent(), "No friendship should exist after decline");

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(alpha.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent(), "Request should be deleted after decline");
  }

  // cancelSentRequest()

  @Test
  public void cancelRequest_toVerified_keepsFollowRelation() {
    createFollow(currentUser, alpha);
    createFriendRequest(currentUser, alpha);

    friendshipService.cancelSentRequest(currentUser, alpha.getUsername());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertFalse(deleted.isPresent(), "Request should be deleted");

    Optional<FollowRelation> kept =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertTrue(kept.isPresent(), "Follow should be kept when receiver is verified");
  }

  // Expired request handling
  @Test
  public void expiredRequest_automaticallyDeletedOnGetIncoming() {
    createFriendRequest(alpha, currentUser);

    FriendRequest expiredRequest =
        FriendRequest.builder()
            .id(new FriendRequestId(beta.getId(), currentUser.getId()))
            .sender(beta)
            .receiver(currentUser)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(expiredRequest);

    List<FriendRequestResponse> incoming = friendshipService.getIncomingRequests(currentUser);

    assertEquals(1, incoming.size());
    assertEquals("alpha", incoming.get(0).username());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(beta.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent(), "Expired request should be deleted");
  }

  @Test
  public void expiredRequest_automaticallyDeletedOnGetSent() {
    createFriendRequest(currentUser, alpha);

    FriendRequest expiredRequest =
        FriendRequest.builder()
            .id(new FriendRequestId(currentUser.getId(), beta.getId()))
            .sender(currentUser)
            .receiver(beta)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(expiredRequest);

    List<FriendRequestResponse> sent = friendshipService.getSentRequests(currentUser);

    assertEquals(1, sent.size());
    assertEquals("alpha", sent.get(0).username());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), beta.getId()));
    assertFalse(deleted.isPresent(), "Expired request should be deleted");
  }

  @Test
  public void expiredRequest_cannotBeAccepted() {
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(alpha.getId(), currentUser.getId()))
            .sender(alpha)
            .receiver(currentUser)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(request);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                friendshipService.handleIncomingRequestAction(
                    currentUser, alpha.getUsername(), FriendRequestAction.ACCEPT));

    assertEquals("This friend request has expired", exception.getMessage());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(alpha.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent(), "Expired request should be deleted");
  }

  @Test
  public void expiredRequest_cannotBeDeclined() {
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(alpha.getId(), currentUser.getId()))
            .sender(alpha)
            .receiver(currentUser)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(request);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                friendshipService.handleIncomingRequestAction(
                    currentUser, alpha.getUsername(), FriendRequestAction.DECLINE));

    assertEquals("This friend request has expired", exception.getMessage());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(alpha.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent(), "Expired request should be deleted");
  }

  @Test
  public void expiredRequest_cannotBeCancelled() {
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(currentUser.getId(), alpha.getId()))
            .sender(currentUser)
            .receiver(alpha)
            .createdAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
    friendRequestRepository.save(request);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> friendshipService.cancelSentRequest(currentUser, alpha.getUsername()));

    assertEquals("This friend request has expired", exception.getMessage());

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertFalse(deleted.isPresent(), "Expired request should be deleted");
  }

  // getPendingRequestCount()
  @Test
  public void getPendingRequestCount_expiredExcluded() {
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

    long count = friendshipService.getPendingRequestCount(currentUser);

    assertEquals(2, count);

    Optional<FriendRequest> deleted =
        friendRequestRepository.findById(new FriendRequestId(gamma.getId(), currentUser.getId()));
    assertFalse(deleted.isPresent(), "Expired request should be deleted");
  }

  @Test
  public void getPendingRequestCount_noRequests_returnsZero() {
    long count = friendshipService.getPendingRequestCount(currentUser);
    assertEquals(0, count);
  }

  // Error handling
  @Test
  public void sendRequest_nonExistentTarget_throwsNotFound() {
    NotFoundException exception =
        assertThrows(
            NotFoundException.class, () -> friendshipService.sendRequest(currentUser, "ghost123"));

    assertEquals("User with identifier 'ghost123' not found.", exception.getMessage());
  }

  @Test
  public void handleRequest_nonExistentSender_throwsNotFound() {
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                friendshipService.handleIncomingRequestAction(
                    currentUser, "ghost123", FriendRequestAction.ACCEPT));

    assertEquals("User with identifier 'ghost123' not found.", exception.getMessage());
  }

  @Test
  public void cancelRequest_nonExistentReceiver_throwsNotFound() {
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> friendshipService.cancelSentRequest(currentUser, "ghost123"));

    assertEquals("User with identifier 'ghost123' not found.", exception.getMessage());
  }

  // real use cases
  @Test
  public void fullFlow_sendThenAccept_createsFriendship() {
    friendshipService.sendRequest(currentUser, alpha.getUsername());

    Optional<FriendRequest> request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertTrue(request.isPresent(), "Request should exist");

    Optional<FollowRelation> currentToReceiver =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertTrue(currentToReceiver.isPresent(), "Current user should follow receiver");

    friendshipService.handleIncomingRequestAction(
        alpha, currentUser.getUsername(), FriendRequestAction.ACCEPT);

    assertTrue(
        friendRelationRepository.existsByUsers(currentUser, alpha), "Friendship should be created");

    request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertFalse(request.isPresent(), "Request should be deleted");
  }

  @Test
  public void fullFlow_sendThenDecline_notFriends() {
    friendshipService.sendRequest(currentUser, alpha.getUsername());

    Optional<FriendRequest> request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertTrue(request.isPresent(), "Request should exist");

    friendshipService.handleIncomingRequestAction(
        alpha, currentUser.getUsername(), FriendRequestAction.DECLINE);

    Optional<FriendRelation> friendship =
        friendRelationRepository.findById(new FriendRelationId(currentUser.getId(), alpha.getId()));

    assertFalse(friendship.isPresent(), "No friendship should exist after decline");

    Optional<FollowRelation> currentToReceiver =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertTrue(currentToReceiver.isPresent(), "Current should still follow receiver");

    request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertFalse(request.isPresent(), "Request should be deleted");
  }

  @Test
  public void sendThenCancel_requestRemoved() {
    friendshipService.sendRequest(currentUser, alpha.getUsername());

    Optional<FriendRequest> request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertTrue(request.isPresent(), "Request should exist");

    friendshipService.cancelSentRequest(currentUser, alpha.getUsername());

    request =
        friendRequestRepository.findById(new FriendRequestId(currentUser.getId(), alpha.getId()));
    assertFalse(request.isPresent(), "Request should be deleted");
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

  // Helper to use assertDoesNotThrow which was added in JUnit 5
  private static void assertDoesNotThrow(
      org.junit.jupiter.api.function.Executable executable, String message) {
    try {
      executable.execute();
    } catch (Throwable t) {
      throw new AssertionError(
          message + " - but threw: " + t.getClass().getName() + ": " + t.getMessage(), t);
    }
  }
}

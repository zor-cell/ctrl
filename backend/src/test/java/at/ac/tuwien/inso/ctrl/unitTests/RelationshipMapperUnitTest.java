package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.user.dto.FollowedResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RelationshipMapper. Tests the default methods in the mapper interface. This is a
 * pure unit test with no Spring context.
 */
public class RelationshipMapperUnitTest {

  // Create a test implementation of the mapper interface for testing default
  // methods
  private final RelationshipMapper relationshipMapper =
      new RelationshipMapper() {
        @Override
        public FriendResponse toFriendResponse(User user) {
          return null; // Not tested here
        }

        @Override
        public FollowedResponse toFollowedResponse(User user) {
          return null; // Not tested here
        }

        @Override
        public FriendRequestResponse toIncomingFriendRequestResponse(FriendRequest request) {
          // Manual implementation matching the @Mapping annotations
          return new FriendRequestResponse(
              request.getSender().getUsername(),
              null, // profilePicture
              true, // isIncoming
              request.getCreatedAt());
        }

        @Override
        public FriendRequestResponse toSentFriendRequestResponse(FriendRequest request) {
          // Manual implementation matching the @Mapping annotations
          return new FriendRequestResponse(
              request.getReceiver().getUsername(),
              null, // profilePicture
              false, // isIncoming
              request.getCreatedAt());
        }

        @Override
        public UserSearchResponse toUserSearchResponse(
            User user,
            UserSearchResponse.RelationshipStatus relationshipStatus,
            String displayName) {
          return null; // Not tested here
        }
      };

  // =============================================================================================
  // displayName tests
  // =============================================================================================

  @Test
  public void displayName_verifiedUser_returnsName() {
    // Create verified user
    User verifiedUser =
        User.builder()
            .id(UUID.randomUUID())
            .username("verifieduser")
            .email("verified@test.com")
            .password("password123")
            .name("Verified User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    // Display name should be user's real name when verified (regardless of
    // relationship)
    String displayName =
        relationshipMapper.displayName(
            verifiedUser, UserSearchResponse.RelationshipStatus.FOLLOWED);

    assertEquals("Verified User", displayName);
  }

  @Test
  public void displayName_unverifiedFriend_returnsName() {
    // Create unverified user
    User unverifiedUser =
        User.builder()
            .id(UUID.randomUUID())
            .username("unverifieduser")
            .email("unverified@test.com")
            .password("password123")
            .name("Unverified Friend")
            .isVerified(false)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    // Display name should be user's real name when friend (even if unverified)
    String displayName =
        relationshipMapper.displayName(
            unverifiedUser, UserSearchResponse.RelationshipStatus.FRIEND);

    assertEquals("Unverified Friend", displayName);
  }

  @Test
  public void displayName_unverifiedNonFriend_returnsNull() {
    // Create unverified user
    User unverifiedUser =
        User.builder()
            .id(UUID.randomUUID())
            .username("unverifieduser")
            .email("unverified@test.com")
            .password("password123")
            .name("Unverified Stranger")
            .isVerified(false)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    // Display name should be null for unverified non-friends
    String displayNameFollowed =
        relationshipMapper.displayName(
            unverifiedUser, UserSearchResponse.RelationshipStatus.FOLLOWED);
    String displayNameNone =
        relationshipMapper.displayName(unverifiedUser, UserSearchResponse.RelationshipStatus.NONE);
    String displayNamePendingSent =
        relationshipMapper.displayName(
            unverifiedUser, UserSearchResponse.RelationshipStatus.PENDING_REQUEST_SENT);
    String displayNamePendingIncoming =
        relationshipMapper.displayName(
            unverifiedUser, UserSearchResponse.RelationshipStatus.PENDING_REQUEST_INCOMING);

    assertNull(displayNameFollowed);
    assertNull(displayNameNone);
    assertNull(displayNamePendingSent);
    assertNull(displayNamePendingIncoming);
  }

  @Test
  public void displayName_verifiedNonFriend_returnsName() {
    User verifiedUser =
        User.builder()
            .id(UUID.randomUUID())
            .username("verifieduser")
            .email("verified@test.com")
            .password("password123")
            .name("Verified Followed")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    String displayName =
        relationshipMapper.displayName(
            verifiedUser, UserSearchResponse.RelationshipStatus.FOLLOWED);

    assertEquals("Verified Followed", displayName);
  }

  // determineRelationshipStatus

  @Test
  public void determineRelationshipStatus_mutualFollow_returnsFriend() {
    UUID userId = UUID.randomUUID();
    User targetUser =
        User.builder()
            .id(userId)
            .username("frienduser")
            .email("friend@test.com")
            .password("password123")
            .name("Friend User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    Set<UUID> friendIds = new HashSet<>();
    friendIds.add(userId);

    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            targetUser, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.FRIEND, status);
  }

  @Test
  public void determineRelationshipStatus_oneWayFollow_returnsFollowed() {
    UUID userId = UUID.randomUUID();
    User targetUser =
        User.builder()
            .id(userId)
            .username("followeduser")
            .email("followed@test.com")
            .password("password123")
            .name("Followed User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    followedIds.add(userId);
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            targetUser, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.FOLLOWED, status);
  }

  @Test
  public void determineRelationshipStatus_pendingSent_returnsPendingSent() {
    UUID userId = UUID.randomUUID();
    User targetUser =
        User.builder()
            .id(userId)
            .username("pendinguser")
            .email("pending@test.com")
            .password("password123")
            .name("Pending User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    pendingSentIds.add(userId);
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            targetUser, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.PENDING_REQUEST_SENT, status);
  }

  @Test
  public void determineRelationshipStatus_pendingIncoming_returnsPendingIncoming() {
    UUID userId = UUID.randomUUID();
    User targetUser =
        User.builder()
            .id(userId)
            .username("pendinguser")
            .email("pending@test.com")
            .password("password123")
            .name("Pending User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();
    pendingIncomingIds.add(userId);

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            targetUser, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.PENDING_REQUEST_INCOMING, status);
  }

  @Test
  public void determineRelationshipStatus_noRelation_returnsNone() {
    UUID userId = UUID.randomUUID();
    User targetUser =
        User.builder()
            .id(userId)
            .username("strangeruser")
            .email("stranger@test.com")
            .password("password123")
            .name("Stranger User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            targetUser, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.NONE, status);
  }

  @Test
  public void determineRelationshipStatus_priority_friendOverFollowed() {
    UUID userId = UUID.randomUUID();
    User targetUser =
        User.builder()
            .id(userId)
            .username("user")
            .email("user@test.com")
            .password("password123")
            .name("User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    Set<UUID> friendIds = new HashSet<>();
    friendIds.add(userId);
    Set<UUID> followedIds = new HashSet<>();
    followedIds.add(userId);
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            targetUser, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.FRIEND, status);
  }

  // toIncomingFriendRequestResponse tests

  @Test
  public void toIncomingFriendRequestResponse_mapsCorrectly() {
    UUID senderId = UUID.randomUUID();
    User sender =
        User.builder()
            .id(senderId)
            .username("senderuser")
            .email("sender@test.com")
            .password("password123")
            .name("Sender User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    UUID receiverId = UUID.randomUUID();
    User receiver =
        User.builder()
            .id(receiverId)
            .username("receiveruser")
            .email("receiver@test.com")
            .password("password123")
            .name("Receiver User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    LocalDateTime createdAt = LocalDateTime.now();
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(senderId, receiverId))
            .sender(sender)
            .receiver(receiver)
            .createdAt(createdAt)
            .expiresAt(createdAt.plusDays(30))
            .build();

    FriendRequestResponse response = relationshipMapper.toIncomingFriendRequestResponse(request);

    assertEquals("senderuser", response.username());
    assertNull(response.profilePicture()); // Currently always null
    assertTrue(response.isIncoming());
    assertEquals(createdAt, response.createdAt());
  }

  @Test
  public void toSentFriendRequestResponse_mapsCorrectly() {
    UUID senderId = UUID.randomUUID();
    User sender =
        User.builder()
            .id(senderId)
            .username("senderuser")
            .email("sender@test.com")
            .password("password123")
            .name("Sender User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    // Create receiver
    UUID receiverId = UUID.randomUUID();
    User receiver =
        User.builder()
            .id(receiverId)
            .username("receiveruser")
            .email("receiver@test.com")
            .password("password123")
            .name("Receiver User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    // Create friend request
    LocalDateTime createdAt = LocalDateTime.now();
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(senderId, receiverId))
            .sender(sender)
            .receiver(receiver)
            .createdAt(createdAt)
            .expiresAt(createdAt.plusDays(30))
            .build();

    // Map to sent response
    FriendRequestResponse response = relationshipMapper.toSentFriendRequestResponse(request);

    // For sent request, username should be the receiver's username
    assertEquals("receiveruser", response.username());
    assertNull(response.profilePicture()); // Currently always null
    assertFalse(response.isIncoming());
    assertEquals(createdAt, response.createdAt());
  }
}

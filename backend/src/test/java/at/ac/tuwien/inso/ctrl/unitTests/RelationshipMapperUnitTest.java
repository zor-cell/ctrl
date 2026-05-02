package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestConfig.class)
@ActiveProfiles("test")
@SpringBootTest
public class RelationshipMapperUnitTest {

  @Autowired private RelationshipMapper relationshipMapper;
  private User alpha;
  private User beta;

  @BeforeEach
  private void setupUsers() {
    alpha =
        User.builder()
            .id(UUID.randomUUID())
            .username("alpha")
            .email("alpha@test.com")
            .password("password123")
            .name("Alpha User")
            .isVerified(true)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();

    beta =
        User.builder()
            .id(UUID.randomUUID())
            .username("beta")
            .email("beta@test.com")
            .password("password123")
            .name("Beta User")
            .isVerified(false)
            .emailVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build();
  }

  @Test
  public void displayName_verifiedUser_returnsName() {
    String displayName =
        relationshipMapper.displayName(alpha, UserSearchResponse.RelationshipStatus.FOLLOWED);

    assertEquals("Alpha User", displayName);
  }

  @Test
  public void displayName_unverifiedFriend_returnsName() {
    String displayName =
        relationshipMapper.displayName(beta, UserSearchResponse.RelationshipStatus.FRIEND);

    assertEquals("Beta User", displayName);
  }

  @Test
  public void displayName_unverifiedNonFriend_returnsNull() {
    String displayNameFollowed =
        relationshipMapper.displayName(beta, UserSearchResponse.RelationshipStatus.FOLLOWED);
    String displayNameNone =
        relationshipMapper.displayName(beta, UserSearchResponse.RelationshipStatus.NONE);
    String displayNamePendingSent =
        relationshipMapper.displayName(
            beta, UserSearchResponse.RelationshipStatus.PENDING_REQUEST_SENT);
    String displayNamePendingIncoming =
        relationshipMapper.displayName(
            beta, UserSearchResponse.RelationshipStatus.PENDING_REQUEST_INCOMING);

    assertNull(displayNameFollowed);
    assertNull(displayNameNone);
    assertNull(displayNamePendingSent);
    assertNull(displayNamePendingIncoming);
  }

  @Test
  public void displayName_verifiedNonFriend_returnsName() {
    String displayName =
        relationshipMapper.displayName(alpha, UserSearchResponse.RelationshipStatus.FOLLOWED);

    assertEquals("Alpha User", displayName);
  }

  // determineRelationshipStatus

  @Test
  public void determineRelationshipStatus_mutualFollow_returnsFriend() {
    Set<UUID> friendIds = new HashSet<>();
    friendIds.add(alpha.getId());

    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.FRIEND, status);
  }

  @Test
  public void determineRelationshipStatus_oneWayFollow_returnsFollowed() {
    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    followedIds.add(alpha.getId());
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.FOLLOWED, status);
  }

  @Test
  public void determineRelationshipStatus_pendingSent_returnsPendingSent() {
    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    pendingSentIds.add(alpha.getId());
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.PENDING_REQUEST_SENT, status);
  }

  @Test
  public void determineRelationshipStatus_pendingIncoming_returnsPendingIncoming() {
    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();
    pendingIncomingIds.add(alpha.getId());

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.PENDING_REQUEST_INCOMING, status);
  }

  @Test
  public void determineRelationshipStatus_noRelation_returnsNone() {
    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.NONE, status);
  }

  @Test
  public void determineRelationshipStatus_priority_friendOverFollowed() {
    Set<UUID> friendIds = new HashSet<>();
    friendIds.add(alpha.getId());
    Set<UUID> followedIds = new HashSet<>();
    followedIds.add(alpha.getId());
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    UserSearchResponse.RelationshipStatus status =
        relationshipMapper.determineRelationshipStatus(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(UserSearchResponse.RelationshipStatus.FRIEND, status);
  }

  // toIncomingFriendRequestResponse tests

  @Test
  public void toIncomingFriendRequestResponse_mapsCorrectly() {
    LocalDateTime createdAt = LocalDateTime.now();
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(alpha.getId(), beta.getId()))
            .sender(alpha)
            .receiver(beta)
            .createdAt(createdAt)
            .expiresAt(createdAt.plusDays(30))
            .build();

    FriendRequestResponse response = relationshipMapper.toIncomingFriendRequestResponse(request);

    assertEquals("alpha", response.username());
    assertTrue(response.isIncoming());
    assertEquals(createdAt, response.createdAt());
  }

  @Test
  public void toSentFriendRequestResponse_mapsCorrectly() {
    LocalDateTime createdAt = LocalDateTime.now();
    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(alpha.getId(), beta.getId()))
            .sender(alpha)
            .receiver(beta)
            .createdAt(createdAt)
            .expiresAt(createdAt.plusDays(30))
            .build();

    FriendRequestResponse response = relationshipMapper.toSentFriendRequestResponse(request);

    assertEquals("beta", response.username());
    assertFalse(response.isIncoming());
    assertEquals(createdAt, response.createdAt());
  }
}

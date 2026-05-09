package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.RelationshipStatusItem;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
            .profilePicture("users/profile-pictures/alpha-avatar.jpg")
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
            .profilePicture(null) // No profile picture
            .build();
  }

  @Test
  public void displayName_verifiedUser_returnsName() {
    List<RelationshipStatusItem> statuses =
        List.of(new RelationshipStatusItem(RelationshipStatusItem.RelationshipStatus.FOLLOWED));
    String displayName = relationshipMapper.displayName(alpha, statuses);

    assertEquals("Alpha User", displayName);
  }

  @Test
  public void displayName_unverifiedFriend_returnsName() {
    List<RelationshipStatusItem> statuses =
        List.of(new RelationshipStatusItem(RelationshipStatusItem.RelationshipStatus.FRIEND));
    String displayName = relationshipMapper.displayName(beta, statuses);

    assertEquals("Beta User", displayName);
  }

  @Test
  public void displayName_unverifiedNonFriend_returnsNull() {
    List<RelationshipStatusItem> statusesFollowed =
        List.of(new RelationshipStatusItem(RelationshipStatusItem.RelationshipStatus.FOLLOWED));
    List<RelationshipStatusItem> statusesNone = Collections.emptyList();
    List<RelationshipStatusItem> statusesPendingSent =
        List.of(
            new RelationshipStatusItem(
                RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_SENT));
    List<RelationshipStatusItem> statusesPendingIncoming =
        List.of(
            new RelationshipStatusItem(
                RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_INCOMING));

    assertNull(relationshipMapper.displayName(beta, statusesFollowed));
    assertNull(relationshipMapper.displayName(beta, statusesNone));
    assertNull(relationshipMapper.displayName(beta, statusesPendingSent));
    assertNull(relationshipMapper.displayName(beta, statusesPendingIncoming));
  }

  @Test
  public void displayName_verifiedNonFriend_returnsName() {
    List<RelationshipStatusItem> statuses =
        List.of(new RelationshipStatusItem(RelationshipStatusItem.RelationshipStatus.FOLLOWED));
    String displayName = relationshipMapper.displayName(alpha, statuses);

    assertEquals("Alpha User", displayName);
  }

  // determineRelationshipStatuses

  private boolean hasStatus(
      List<RelationshipStatusItem> statuses, RelationshipStatusItem.RelationshipStatus status) {
    return statuses.stream().anyMatch(item -> item.status() == status);
  }

  @Test
  public void determineRelationshipStatuses_friend_returnsFriend() {
    Set<UUID> friendIds = new HashSet<>();
    friendIds.add(alpha.getId());

    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(1, statuses.size());
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.FRIEND));
  }

  @Test
  public void determineRelationshipStatuses_followed_returnsFollowed() {
    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    followedIds.add(alpha.getId());
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(1, statuses.size());
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.FOLLOWED));
  }

  @Test
  public void determineRelationshipStatuses_pendingSent_returnsPendingSent() {
    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    pendingSentIds.add(alpha.getId());
    Set<UUID> pendingIncomingIds = new HashSet<>();

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(1, statuses.size());
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_SENT));
  }

  @Test
  public void determineRelationshipStatuses_pendingIncoming_returnsPendingIncoming() {
    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();
    pendingIncomingIds.add(alpha.getId());

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(1, statuses.size());
    assertTrue(
        hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_INCOMING));
  }

  @Test
  public void determineRelationshipStatuses_noRelation_returnsEmptyList() {
    Set<UUID> friendIds = new HashSet<>();
    Set<UUID> followedIds = new HashSet<>();
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertTrue(statuses.isEmpty());
  }

  @Test
  public void determineRelationshipStatuses_friendAndFollowed_returnsBoth() {
    Set<UUID> friendIds = new HashSet<>();
    friendIds.add(alpha.getId());
    Set<UUID> followedIds = new HashSet<>();
    followedIds.add(alpha.getId());
    Set<UUID> pendingSentIds = new HashSet<>();
    Set<UUID> pendingIncomingIds = new HashSet<>();

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, followedIds, friendIds, pendingSentIds, pendingIncomingIds);

    assertEquals(2, statuses.size());
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.FRIEND));
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.FOLLOWED));
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

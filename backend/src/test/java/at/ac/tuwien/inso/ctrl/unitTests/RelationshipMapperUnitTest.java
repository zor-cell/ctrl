package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.RelationshipStatusItem;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
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

public class RelationshipMapperUnitTest {

  private RelationshipMapper relationshipMapper;
  private User alpha;
  private User beta;

  @BeforeEach
  void setupUsers() {
    relationshipMapper = new RelationshipMapper();

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
            .profilePicture(null)
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

  @Test
  public void determineRelationshipStatuses_friend_returnsFriend() {
    Set<UUID> friendIds = new HashSet<>();
    friendIds.add(alpha.getId());

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, Set.of(), friendIds, Set.of(), Set.of());

    assertEquals(1, statuses.size());
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.FRIEND));
  }

  @Test
  public void determineRelationshipStatuses_followed_returnsFollowed() {
    Set<UUID> followedIds = new HashSet<>();
    followedIds.add(alpha.getId());

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, followedIds, Set.of(), Set.of(), Set.of());

    assertEquals(1, statuses.size());
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.FOLLOWED));
  }

  @Test
  public void determineRelationshipStatuses_pendingSent_returnsPendingSent() {
    Set<UUID> pendingSentIds = new HashSet<>();
    pendingSentIds.add(alpha.getId());

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, Set.of(), Set.of(), pendingSentIds, Set.of());

    assertEquals(1, statuses.size());
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_SENT));
  }

  @Test
  public void determineRelationshipStatuses_pendingIncoming_returnsPendingIncoming() {
    Set<UUID> pendingIncomingIds = new HashSet<>();
    pendingIncomingIds.add(alpha.getId());

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, Set.of(), Set.of(), Set.of(), pendingIncomingIds);

    assertEquals(1, statuses.size());
    assertTrue(
        hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_INCOMING));
  }

  @Test
  public void determineRelationshipStatuses_noRelation_returnsEmptyList() {
    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, Set.of(), Set.of(), Set.of(), Set.of());

    assertTrue(statuses.isEmpty());
  }

  @Test
  public void determineRelationshipStatuses_friendAndFollowed_returnsBoth() {
    Set<UUID> friendIds = new HashSet<>();
    friendIds.add(alpha.getId());
    Set<UUID> followedIds = new HashSet<>();
    followedIds.add(alpha.getId());

    List<RelationshipStatusItem> statuses =
        relationshipMapper.determineRelationshipStatuses(
            alpha, followedIds, friendIds, Set.of(), Set.of());

    assertEquals(2, statuses.size());
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.FRIEND));
    assertTrue(hasStatus(statuses, RelationshipStatusItem.RelationshipStatus.FOLLOWED));
  }

  @Test
  public void toUserSearchResponse_withProfilePicture_resolvesProfilePictureUrl() {
    List<RelationshipStatusItem> statuses =
        List.of(new RelationshipStatusItem(RelationshipStatusItem.RelationshipStatus.FRIEND));

    UserSearchResponse response =
        relationshipMapper.toUserSearchResponse(alpha, statuses, "Alpha User");

    assertEquals(alpha.getId(), response.id());
    assertEquals("alpha", response.username());
    assertEquals("Alpha User", response.name());
    assertEquals("users/profile-pictures/alpha-avatar.jpg", response.profilePicture());
    assertTrue(response.isVerified());
    assertEquals(statuses, response.relationshipStatuses());
  }

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
    assertEquals("users/profile-pictures/alpha-avatar.jpg", response.profilePicture());
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
    assertNull(response.profilePicture());
    assertFalse(response.isIncoming());
    assertEquals(createdAt, response.createdAt());
  }

  private boolean hasStatus(
      List<RelationshipStatusItem> statuses, RelationshipStatusItem.RelationshipStatus status) {
    return statuses.stream().anyMatch(item -> item.status() == status);
  }

}

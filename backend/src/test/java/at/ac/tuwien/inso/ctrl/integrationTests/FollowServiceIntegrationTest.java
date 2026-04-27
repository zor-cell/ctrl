package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.user.dto.FollowedResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for FollowService using shared test users. Tests service methods directly with
 * real database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestConfig.class)
@ActiveProfiles("test")
@Transactional
public class FollowServiceIntegrationTest {

  @Autowired private FollowService followService;

  @Autowired private UserRepository userRepository;

  @Autowired private FollowRelationRepository followRelationRepository;

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
    followRelationRepository.deleteAll();
    userRepository.deleteAll();
  }

  // getFriends()

  @Test
  public void getFriends_returnsMutualFollows() {
    createMutualFollows(currentUser, alpha);
    createMutualFollows(currentUser, beta);

    List<FriendResponse> friends = followService.getFriends(currentUser, null);

    assertEquals(2, friends.size());
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("alpha")));
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("beta")));
  }

  @Test
  public void getFriends_doesNotReturnOneWayFollows() {
    createFollow(currentUser, alpha);
    createFollow(beta, currentUser);

    List<FriendResponse> friends = followService.getFriends(currentUser, null);

    assertTrue(friends.isEmpty());
  }

  @Test
  public void getFriends_withSearchFilters_correctlyReturns() {
    createMutualFollows(currentUser, alpha);
    createMutualFollows(currentUser, beta);
    createMutualFollows(currentUser, delta);

    List<FriendResponse> friends = followService.getFriends(currentUser, "ta");

    assertEquals(2, friends.size());
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("beta")));
    assertTrue(friends.stream().anyMatch(f -> f.username().equals("delta")));
  }

  @Test
  public void getFriends_withoutFriends_returnsEmptyResult() {
    List<FriendResponse> friends = followService.getFriends(currentUser, null);
    assertTrue(friends.isEmpty());
  }

  // getFollowed()

  @Test
  public void getFollowed_returnsAllFollowedUsersButNoFriends() {
    createFollow(currentUser, alpha);
    createFollow(currentUser, beta);
    createMutualFollows(currentUser, gamma);

    List<FollowedResponse> followed = followService.getFollowed(currentUser, null);

    assertEquals(2, followed.size());
    assertTrue(followed.stream().anyMatch(f -> f.username().equals("alpha")));
    assertTrue(followed.stream().anyMatch(f -> f.username().equals("beta")));
    assertFalse(followed.stream().anyMatch(f -> f.username().equals("gamma")));
  }

  @Test
  public void getFollowed_withSearchFilters_correctlyReturns() {
    createFollow(currentUser, alpha);
    createFollow(currentUser, beta);

    List<FollowedResponse> followed = followService.getFollowed(currentUser, "be");

    assertEquals(1, followed.size());
    assertEquals("beta", followed.get(0).username());
  }

  @Test
  public void getFollowed_withoutFollowes_returnsNone() {
    List<FriendResponse> friends = followService.getFriends(currentUser, null);
    assertTrue(friends.isEmpty());
  }

  // areFriends()

  @Test
  public void areFriends_returnsTrueForMutualFollow() {
    createMutualFollows(currentUser, alpha);

    assertTrue(followService.areFriends(currentUser, alpha));
    assertTrue(followService.areFriends(alpha, currentUser));
  }

  @Test
  public void areFriends_returnsFalseForOneWayFollow() {
    createFollow(currentUser, alpha);
    createFollow(beta, currentUser);

    assertFalse(followService.areFriends(currentUser, alpha));
    assertFalse(followService.areFriends(alpha, currentUser));
    assertFalse(followService.areFriends(currentUser, beta));
    assertFalse(followService.areFriends(beta, currentUser));
  }

  @Test
  public void areFriends_returnsFalseForNoRelation() {
    assertFalse(followService.areFriends(currentUser, alpha));
    assertFalse(followService.areFriends(alpha, currentUser));
  }

  // unfollow()

  @Test
  public void unfollow_removesBothFollowRelations() {
    createMutualFollows(currentUser, alpha);

    followService.unfollow(currentUser, alpha.getUsername());

    Optional<FollowRelation> currentToAlpha =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    Optional<FollowRelation> alphaToCurrent =
        followRelationRepository.findById(new FollowRelationId(alpha.getId(), currentUser.getId()));

    assertFalse(currentToAlpha.isPresent());
    assertFalse(alphaToCurrent.isPresent());
  }

  @Test
  public void unfollow_oneWayFollow_removesFollowRelation() {
    createFollow(currentUser, alpha);

    followService.unfollow(currentUser, alpha.getUsername());

    Optional<FollowRelation> currentToAlpha =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertFalse(currentToAlpha.isPresent());
  }

  // follow()

  @Test
  public void follow_isIdempotent() {
    final UUID currentUserId = currentUser.getId();
    final UUID alphaId = alpha.getId();

    followService.follow(currentUser, alpha.getUsername());

    List<FollowRelation> follows = followRelationRepository.findAll();
    long count =
        follows.stream()
            .filter(
                f ->
                    f.getFollower().getId().equals(currentUserId)
                        && f.getFollowed().getId().equals(alphaId))
            .count();
    assertEquals(1, count, "Should have exactly one follow relation");

    followService.follow(currentUser, alpha.getUsername());

    // Verify idempotency
    follows = followRelationRepository.findAll();
    count =
        follows.stream()
            .filter(
                f ->
                    f.getFollower().getId().equals(currentUserId)
                        && f.getFollowed().getId().equals(alphaId))
            .count();
    assertEquals(1, count, "Should still have exactly one follow relation (idempotent)");
  }

  // getFollowedIds()

  @Test
  public void getFollowedIds_returnsCorrectSet() {
    createFollow(currentUser, alpha);
    createFollow(currentUser, beta);

    Set<UUID> followedIds = followService.getFollowedIds(currentUser);

    assertEquals(2, followedIds.size());
    assertTrue(followedIds.contains(alpha.getId()));
    assertTrue(followedIds.contains(beta.getId()));
    assertFalse(followedIds.contains(gamma.getId()));
    assertFalse(followedIds.contains(delta.getId()));
  }

  @Test
  public void getFollowedIds_emptyResult() {
    Set<UUID> followedIds = followService.getFollowedIds(currentUser);
    assertTrue(followedIds.isEmpty());
  }

  // getFriendIds()

  @Test
  public void getFriendIds_returnsCorrectSet() {
    createMutualFollows(currentUser, alpha);
    createMutualFollows(currentUser, gamma);

    createFollow(currentUser, beta);

    Set<UUID> friendIds = followService.getFriendIds(currentUser);

    // Should only include mutual friends, not one-way follows
    assertEquals(2, friendIds.size());
    assertTrue(friendIds.contains(alpha.getId()));
    assertTrue(friendIds.contains(gamma.getId()));
    assertFalse(friendIds.contains(beta.getId()));
  }

  // isFollowing()
  @Test
  public void isFollowing_returnsTrueWhenFollowing() {
    createFollow(currentUser, alpha);

    assertTrue(followService.isFollowing(currentUser, alpha));
  }

  @Test
  public void isFollowing_returnsFalseWhenNotFollowing() {
    assertFalse(followService.isFollowing(currentUser, alpha));
  }

  @Test
  public void isFollowing_returnsFalseForReverseFollow() {
    createFollow(alpha, currentUser);

    assertFalse(followService.isFollowing(currentUser, alpha));
    assertTrue(followService.isFollowing(alpha, currentUser));
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
}

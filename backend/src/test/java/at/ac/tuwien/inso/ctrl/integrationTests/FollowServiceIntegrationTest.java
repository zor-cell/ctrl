package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.user.dto.FollowedResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
public class FollowServiceIntegrationTest {

  @Autowired private FollowService followService;

  @Autowired private UserRepository userRepository;

  @Autowired private FollowRelationRepository followRelationRepository;

  @Autowired private FriendRelationRepository friendRelationRepository;

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
            .profilePicture("users/profile-pictures/current-avatr.jpg")
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
            .profilePicture("users/profile-pictures/beta-avatar.jpg")
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
            .profilePicture(null) // No profile picture
            .build();
    delta = userRepository.save(delta);
  }

  @AfterEach
  public void tearDownTestData() {
    followRelationRepository.deleteAll();
    userRepository.deleteAll();
  }

  // getFollowed()

  @Test
  public void getFollowed_onlyReturnsAllFollowedUsers() {
    createFollow(currentUser, alpha);
    createFollow(beta, currentUser);
    createFriendship(currentUser, gamma);

    List<FollowedResponse> followed = followService.getFollowed(currentUser, null);

    assertEquals(1, followed.size());
    assertTrue(followed.stream().anyMatch(f -> f.username().equals("alpha")));
    assertFalse(followed.stream().anyMatch(f -> f.username().equals("beta")));
    assertFalse(followed.stream().anyMatch(f -> f.username().equals("gamma")));
    assertFalse(followed.stream().anyMatch(f -> f.username().equals("delta")));
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
    List<FollowedResponse> followed = followService.getFollowed(currentUser, null);
    assertTrue(followed.isEmpty());
  }

  // unfollow()
  @Test
  public void unfollow_oneWayFollow_removesFollowRelation() {
    createFollow(currentUser, alpha);

    followService.unfollow(currentUser, alpha.getUsername());

    entityManager.flush();
    entityManager.clear();

    Optional<FollowRelation> currentToAlpha =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    assertFalse(currentToAlpha.isPresent());
  }

  @Test
  public void unfollow_whenFollowed_doesNotRemoveFollowingRelation() {
    createFollow(currentUser, alpha);
    createFollow(alpha, currentUser);

    followService.unfollow(currentUser, alpha.getUsername());

    entityManager.flush();
    entityManager.clear();

    Optional<FollowRelation> currentToAlpha =
        followRelationRepository.findById(new FollowRelationId(currentUser.getId(), alpha.getId()));
    Optional<FollowRelation> alphaToCurrent =
        followRelationRepository.findById(new FollowRelationId(alpha.getId(), currentUser.getId()));

    assertFalse(currentToAlpha.isPresent());
    assertTrue(alphaToCurrent.isPresent());
  }

  @Test
  public void unfollow_whenFriends_throwsNotFoundExceptionAndPreservesFriendship() {
    createFriendship(currentUser, alpha);

    assertThrowsExactly(
        NotFoundException.class, () -> followService.unfollow(currentUser, alpha.getUsername()));

    assertTrue(friendRelationRepository.existsByUsers(alpha, currentUser));
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
    createFollow(beta, currentUser);
    createFriendship(currentUser, gamma);

    Set<UUID> followedIds = followService.getFollowedIds(currentUser);

    assertEquals(1, followedIds.size());
    assertTrue(followedIds.contains(alpha.getId()));
    assertFalse(followedIds.contains(beta.getId()));
    assertFalse(followedIds.contains(gamma.getId()));
    assertFalse(followedIds.contains(delta.getId()));
  }

  @Test
  public void getFollowedIds_emptyResult() {
    Set<UUID> followedIds = followService.getFollowedIds(currentUser);
    assertTrue(followedIds.isEmpty());
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

  @Test
  public void isFollowing_whenFriends_returnsFalse() {
    createFriendship(currentUser, alpha);

    assertFalse(followService.isFollowing(currentUser, alpha));
    assertFalse(followService.isFollowing(alpha, currentUser));
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

  private void createFriendship(User user1, User user2) {
    FriendRelation relation =
        FriendRelation.builder()
            .id(new FriendRelationId(user1.getId(), user2.getId()))
            .friend1(user1)
            .friend2(user2)
            .since(LocalDateTime.now())
            .build();
    friendRelationRepository.save(relation);
  }
}

package at.ac.tuwien.inso.ctrl.user.repository;

import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRelationRepository extends JpaRepository<FollowRelation, FollowRelationId> {

  @Query("SELECT fr.following FROM FollowRelation fr WHERE fr.follower = :follower")
  List<User> findFollowingByFollower(@Param("follower") User follower);

  @Query(
      """
      SELECT u FROM User u
      JOIN FollowRelation fr1 ON fr1.following = u AND fr1.follower = :user
      JOIN FollowRelation fr2 ON fr2.follower = u AND fr2.following = :user
      """)
  List<User> findFriends(@Param("user") User user);

  // potential performance bottleneck: search via LIKE
  @Query(
      """
      SELECT u FROM User u
      JOIN FollowRelation fr1 ON fr1.following = u AND fr1.follower = :user
      JOIN FollowRelation fr2 ON fr2.follower = u AND fr2.following = :user
      WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(u.name)     LIKE LOWER(CONCAT('%', :search, '%'))
      """)
  List<User> findFriendsBySearch(@Param("user") User user, @Param("search") String search);

  // potential performance bottleneck: search via LIKE
  @Query(
      """
      SELECT fr.following FROM FollowRelation fr
      WHERE fr.follower = :follower
      AND LOWER(fr.following.username) LIKE LOWER(CONCAT('%', :search, '%'))
      """)
  List<User> findFollowingBySearch(
      @Param("follower") User follower, @Param("search") String search);

  boolean existsByFollowerAndFollowing(User follower, User following);

  @Modifying
  @Query(
      "DELETE FROM FollowRelation fr WHERE fr.follower = :follower AND fr.following = :following")
  void deleteByFollowerAndFollowing(
      @Param("follower") User follower, @Param("following") User following);

  @Modifying
  @Query(
      """
      DELETE FROM FollowRelation fr
      WHERE (fr.follower = :user1 AND fr.following = :user2)
         OR (fr.follower = :user2 AND fr.following = :user1)
      """)
  void deleteBidirectional(@Param("user1") User user1, @Param("user2") User user2);

  @Query("SELECT fr.following.id FROM FollowRelation fr WHERE fr.follower = :user")
  Set<UUID> findFollowingIdsByFollower(@Param("user") User user);

  @Query(
      """
      SELECT u.id FROM User u
      JOIN FollowRelation fr1 ON fr1.following = u AND fr1.follower = :user
      JOIN FollowRelation fr2 ON fr2.follower = u AND fr2.following = :user
      """)
  Set<UUID> findFriendIds(@Param("user") User user);
}

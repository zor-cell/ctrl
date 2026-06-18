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

  @Query(
      """
        SELECT fr.followed FROM FollowRelation fr WHERE fr.follower = :follower AND fr.followed.isSuspended = false
      """)
  List<User> findFollowedByFollower(@Param("follower") User follower);

  // potential performance bottleneck: search via LIKE
  @Query(
      """
      SELECT fr.followed FROM FollowRelation fr
      WHERE fr.follower = :follower
      AND fr.followed.isSuspended = false
      AND fr.followed.username LIKE CONCAT('%', :search, '%')
      """)
  List<User> findFollowedBySearch(@Param("follower") User follower, @Param("search") String search);

  boolean existsByFollowerAndFollowed(User follower, User followed);

  @Modifying
  @Query("DELETE FROM FollowRelation fr WHERE fr.follower = :follower AND fr.followed = :followed")
  int deleteByFollowerAndFollowed(
      @Param("follower") User follower, @Param("followed") User followed);

  @Modifying
  @Query("DELETE FROM FollowRelation fr WHERE fr.follower = :user OR fr.followed = :user")
  int deleteAllByUser(@Param("user") User user);

  @Modifying
  @Query(
      """
      DELETE FROM FollowRelation fr
      WHERE (fr.follower = :user1 AND fr.followed = :user2)
         OR (fr.follower = :user2 AND fr.followed = :user1)
      """)
  void deleteBidirectional(@Param("user1") User user1, @Param("user2") User user2);

  @Query(
      """
      SELECT fr.followed.id FROM FollowRelation fr WHERE fr.follower = :user AND fr.followed.isSuspended = false
      """)
  Set<UUID> findFollowedIdsByFollower(@Param("user") User user);

  // All user IDs that the user follows directly
  @Query("SELECT f.id.followedId FROM FollowRelation f WHERE f.id.followerId = :userId")
  Set<UUID> findFollowedIds(UUID userId);

  // IDs of the accounts that the followers follow (for Extended Network)
  @Query(
      value =
          """
    SELECT DISTINCT target_id FROM (
        -- follows.follows
        SELECT f2.followed_id AS target_id
        FROM follow_relations f1
        JOIN follow_relations f2 ON f1.followed_id = f2.follower_id
        WHERE f1.follower_id = :userId

        UNION

        -- follows.friends
        SELECT CASE WHEN fr.friend_id_1 = f1.followed_id THEN fr.friend_id_2 ELSE fr.friend_id_1 END AS target_id
        FROM follow_relations f1
        JOIN friend_relation fr ON f1.followed_id IN (fr.friend_id_1, fr.friend_id_2)
        WHERE f1.follower_id = :userId

        UNION

        -- friends.follows
        SELECT f.followed_id AS target_id
        FROM friend_relation fr
        JOIN follow_relations f ON CASE WHEN fr.friend_id_1 = :userId THEN fr.friend_id_2 ELSE fr.friend_id_1 END = f.follower_id
        WHERE fr.friend_id_1 = :userId OR fr.friend_id_2 = :userId

        UNION

        -- friends.friends
        SELECT CASE WHEN fr2.friend_id_1 = friends.fr1_friend THEN fr2.friend_id_2 ELSE fr2.friend_id_1 END AS target_id
        FROM (
            SELECT CASE WHEN fr1.friend_id_1 = :userId THEN fr1.friend_id_2 ELSE fr1.friend_id_1 END AS fr1_friend
            FROM friend_relation fr1
            WHERE fr1.friend_id_1 = :userId OR fr1.friend_id_2 = :userId
        ) friends
        JOIN friend_relation fr2 ON fr2.friend_id_1 = friends.fr1_friend OR fr2.friend_id_2 = friends.fr1_friend
    ) AS second_degree
    """,
      nativeQuery = true)
  Set<UUID> findSecondDegreeIds(@Param("userId") UUID userId);
}

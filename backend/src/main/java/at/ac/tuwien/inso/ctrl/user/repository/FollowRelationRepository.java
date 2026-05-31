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
}

package at.ac.tuwien.inso.ctrl.user.repository;

import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
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
public interface FriendRelationRepository extends JpaRepository<FriendRelation, FriendRelationId> {

  @Query(
      """
      SELECT u FROM User u
      JOIN FriendRelation fr ON
        (fr.friend1 = :user AND fr.friend2 = u)
        OR (fr.friend2 = :user AND fr.friend1 = u)
      """)
  List<User> findFriends(@Param("user") User user);

  @Query(
      """
      SELECT u FROM User u
      JOIN FriendRelation fr ON
        (fr.friend1 = :user AND fr.friend2 = u)
        OR (fr.friend2 = :user AND fr.friend1 = u)
      WHERE u.username LIKE CONCAT('%', :search, '%')
         OR u.name LIKE CONCAT('%', :search, '%')
      """)
  List<User> findFriendsBySearch(@Param("user") User user, @Param("search") String search);

  @Modifying
  @Query(
      """
      DELETE FROM FriendRelation fr
      WHERE (fr.friend1 = :user1 AND fr.friend2 = :user2)
         OR (fr.friend1 = :user2 AND fr.friend2 = :user1)
      """)
  void deleteFriendship(@Param("user1") User user1, @Param("user2") User user2);

  @Query(
      """
      SELECT CASE WHEN COUNT(fr) > 0 THEN true ELSE false END
      FROM FriendRelation fr
      WHERE (fr.friend1 = :user1 AND fr.friend2 = :user2)
         OR (fr.friend1 = :user2 AND fr.friend2 = :user1)
      """)
  boolean existsByUsers(@Param("user1") User user1, @Param("user2") User user2);

  @Query(
      """
      SELECT CASE WHEN fr.friend1 = :user THEN fr.friend2.id ELSE fr.friend1.id END
      FROM FriendRelation fr
      WHERE fr.friend1 = :user OR fr.friend2 = :user
      """)
  Set<UUID> findFriendIds(@Param("user") User user);
}

package at.ac.tuwien.inso.ctrl.user.repository;

import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelation;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockedRelationRepository
    extends JpaRepository<BlockedRelation, BlockedRelationId> {
  @Query(
      """
      SELECT br.blockedUser FROM BlockedRelation br
      WHERE br.user = :user
      AND br.blockedUser.isSuspended = false
      """)
  List<User> findBlockedUserByUser(@Param("user") User user);

  boolean existsBlockedRelationByUserAndBlockedUser(
      @Param("user") User user, @Param("blockedUser") User blockedUser);

  @Query(
      """
      SELECT br.blockedUser FROM BlockedRelation br
      WHERE br.user = :user
      AND br.blockedUser.isSuspended = false
      AND br.blockedUser.username LIKE CONCAT('%', :search, '%')
      """)
  List<User> findBlockedUserByUserAndSearch(User user, String search);
}

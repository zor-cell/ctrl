package at.ac.tuwien.inso.ctrl.user.repository;

import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, FriendRequestId> {

  @Query(
      """
        SELECT fr FROM FriendRequest fr WHERE fr.receiver = :receiver AND fr.sender.isSuspended = false
      """)
  List<FriendRequest> findAllIncomingRequests(@Param("receiver") User receiver);

  @Query(
      """
      SELECT fr FROM FriendRequest fr WHERE fr.sender = :sender AND fr.receiver.isSuspended = false
      """)
  List<FriendRequest> findAllSentRequests(@Param("sender") User sender);

  @Query("SELECT fr FROM FriendRequest fr WHERE fr.sender = :sender AND fr.receiver = :receiver")
  Optional<FriendRequest> findBySenderAndReceiver(
      @Param("sender") User sender, @Param("receiver") User receiver);

  boolean existsBySenderAndReceiver(User sender, User receiver);

  @Modifying
  @Query("DELETE FROM FriendRequest fr WHERE fr.sender = :user OR fr.receiver = :user")
  int deleteAllByUser(@Param("user") User user);

  @Query(
      """
      SELECT fr.sender.id FROM FriendRequest fr WHERE fr.receiver = :receiver AND fr.sender.isSuspended = false
      """)
  Set<UUID> findSenderIdsByReceiver(@Param("receiver") User receiver);

  @Query(
      """
      SELECT fr.receiver.id FROM FriendRequest fr WHERE fr.sender = :sender AND fr.receiver.isSuspended = false
      """)
  Set<UUID> findReceiverIdsBySender(@Param("sender") User sender);
}

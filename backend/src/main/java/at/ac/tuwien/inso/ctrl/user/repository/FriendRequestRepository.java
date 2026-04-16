package at.ac.tuwien.inso.ctrl.user.repository;

import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

  @Query("SELECT fr FROM FriendRequest fr WHERE fr.receiver = :receiver")
  List<FriendRequest> findAllIncomingRequests(@Param("receiver") User receiver);

  @Query("SELECT fr FROM FriendRequest fr WHERE fr.sender = :sender")
  List<FriendRequest> findAllSentRequests(@Param("sender") User sender);

  boolean existsBySenderAndReceiver(User sender, User receiver);

  @Query("SELECT fr.sender.id FROM FriendRequest fr WHERE fr.receiver = :receiver")
  Set<UUID> findSenderIdsByReceiver(@Param("receiver") User receiver);

  @Query("SELECT fr.receiver.id FROM FriendRequest fr WHERE fr.sender = :sender")
  Set<UUID> findReceiverIdsBySender(@Param("sender") User sender);
}

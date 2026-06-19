package at.ac.tuwien.inso.ctrl.chat.repository;

import at.ac.tuwien.inso.ctrl.chat.entity.Reaction;
import at.ac.tuwien.inso.ctrl.chat.entity.ReactionId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReactionRepository extends JpaRepository<Reaction, ReactionId> {
  @Query(
      "SELECT r FROM Reaction r WHERE r.fromUser.id = :userId AND r.message.messageId = :messageId")
  List<Reaction> findAllByUserIdAndMessageId(
      @Param("userId") UUID userId, @Param("messageId") UUID messageId);

  @Modifying(flushAutomatically = true)
  @Query(
      "DELETE FROM Reaction r WHERE r.fromUser.id = :userId AND r.message.messageId = :messageId")
  int deleteAllByUserIdAndMessageId(
      @Param("userId") UUID userId, @Param("messageId") UUID messageId);

  @Modifying
  @Query("DELETE FROM Reaction r WHERE r.fromUser.id = :userId")
  int deleteAllByUserId(@Param("userId") UUID userId);
}

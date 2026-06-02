package at.ac.tuwien.inso.ctrl.chat.repository;

import at.ac.tuwien.inso.ctrl.chat.entity.Chat;
import at.ac.tuwien.inso.ctrl.chat.entity.Message;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {
  @EntityGraph(
      attributePaths = {
        "reactions",
        "reactions.fromUser",
        "shared",
        "shared.post",
        "shared.post.user",
        "shared.post.externalAuthor"
      })
  List<Message> findByChatOrderByDateAsc(Chat chat);

  @EntityGraph(
      attributePaths = {
        "reactions",
        "reactions.fromUser",
        "fromUser",
        "shared",
        "shared.post",
        "shared.post.user",
        "shared.post.externalAuthor"
      })
  Optional<Message> findTopByChatOrderByDateDesc(Chat chat);

  @EntityGraph(
      attributePaths = {
        "reactions",
        "reactions.fromUser",
        "fromUser",
        "chat",
        "shared",
        "shared.post",
        "shared.post.user",
        "shared.post.externalAuthor"
      })
  Optional<Message> findWithDetailsByMessageId(UUID messageId);

  @Query(
      """
      SELECT COUNT(m) FROM Message m
      WHERE m.chat = :chat
        AND m.fromUser <> :user
        AND m.system = false
      """)
  long countUnreadMessagesWhenNeverRead(@Param("chat") Chat chat, @Param("user") User user);

  @Query(
      """
      SELECT COUNT(m) FROM Message m
      WHERE m.chat = :chat
        AND m.fromUser <> :user
        AND m.system = false
        AND m.date > :lastReadAt
      """)
  long countUnreadMessagesSince(
      @Param("chat") Chat chat,
      @Param("user") User user,
      @Param("lastReadAt") LocalDateTime lastReadAt);
}

package at.ac.tuwien.inso.ctrl.chat.repository;

import at.ac.tuwien.inso.ctrl.chat.entity.Chat;
import at.ac.tuwien.inso.ctrl.chat.entity.Message;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {
  interface UserMessageCount {
    UUID getUserId();

    long getMessageCount();
  }

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

  @Query(
      """
      SELECT m.messageId FROM Message m
      WHERE m.chat = :chat
      ORDER BY m.date DESC
      """)
  List<UUID> findLatestMessageIds(@Param("chat") Chat chat, Pageable pageable);

  @Query(
      """
      SELECT m.messageId FROM Message m
      WHERE m.chat = :chat
        AND m.date < :before
      ORDER BY m.date DESC
      """)
  List<UUID> findMessageIdsBefore(
      @Param("chat") Chat chat, @Param("before") LocalDateTime before, Pageable pageable);

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
  @Query("SELECT m FROM Message m WHERE m.messageId IN :messageIds")
  List<Message> findMessagesWithDetailsByIds(@Param("messageIds") List<UUID> messageIds);

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

  @Query(
      """
      SELECT COUNT(m) FROM Message m
      WHERE m.chat = :chat
        AND m.fromUser = :user
        AND m.system = false
      """)
  long countPersonalMessages(@Param("chat") Chat chat, @Param("user") User user);

  @Query(
      """
      SELECT m.fromUser.id AS userId, COUNT(m) AS messageCount
      FROM Message m
      WHERE m.chat = :chat
        AND m.fromUser.id IN :userIds
        AND m.system = false
      GROUP BY m.fromUser.id
      """)
  List<UserMessageCount> countPersonalMessagesByUsers(
      @Param("chat") Chat chat, @Param("userIds") Collection<UUID> userIds);

  @Query(
      """
      SELECT member.user.id AS userId, COUNT(message) AS messageCount
      FROM ChatMember member
      LEFT JOIN member.chat.messages message
        ON message.fromUser <> member.user
        AND message.system = false
        AND (member.lastReadAt IS NULL OR message.date > member.lastReadAt)
      WHERE member.chat = :chat
        AND member.user.id IN :userIds
        AND member.leftAt IS NULL
      GROUP BY member.user.id
      """)
  List<UserMessageCount> countUnreadMessagesByUsers(
      @Param("chat") Chat chat, @Param("userIds") Collection<UUID> userIds);
}

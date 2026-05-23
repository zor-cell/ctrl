package at.ac.tuwien.inso.ctrl.chat.repository;

import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMemberId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {
  @Query(
      """
      SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
      FROM ChatMember m
      WHERE m.chat.chatId = :chatId AND m.user = :user AND m.leftAt IS NULL
      """)
  boolean existsActiveByChatChatIdAndUser(@Param("chatId") UUID chatId, @Param("user") User user);

  Optional<ChatMember> findByChatChatIdAndUser(UUID chatId, User user);

  @Query(
      "SELECT DISTINCT m.chat.chatId FROM ChatMember m WHERE m.user.id = :userId AND m.leftAt IS NULL")
  List<UUID> findChatIdsForUser(@Param("userId") UUID userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM ChatMember m WHERE m.chat.chatId = :chatId AND m.user.id = :userId")
  int deleteByChatIdAndUserId(@Param("chatId") UUID chatId, @Param("userId") UUID userId);
}

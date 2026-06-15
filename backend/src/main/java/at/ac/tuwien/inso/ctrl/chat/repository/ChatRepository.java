package at.ac.tuwien.inso.ctrl.chat.repository;

import at.ac.tuwien.inso.ctrl.chat.entity.Chat;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
  Optional<Chat> findByChatId(UUID chatId);

  @Query(
      """
      SELECT DISTINCT c FROM Chat c
      LEFT JOIN FETCH c.owner
      LEFT JOIN FETCH c.members member
      LEFT JOIN FETCH member.user
      WHERE member.user = :user
      """)
  List<Chat> findChatsForUser(@Param("user") User user);

  @Query(
      """
      SELECT DISTINCT c FROM Chat c
      LEFT JOIN FETCH c.owner
      LEFT JOIN FETCH c.members member
      LEFT JOIN FETCH member.user
      WHERE EXISTS (
        SELECT 1 FROM ChatMember currentMember
        WHERE currentMember.chat = c
          AND currentMember.user.id = :userId
          AND currentMember.leftAt IS NULL
      )
      """)
  List<Chat> findActiveChatsForUserWithDetails(@Param("userId") UUID userId);

  @Query(
      """
      SELECT c FROM Chat c
      LEFT JOIN FETCH c.owner
      LEFT JOIN FETCH c.members member
      LEFT JOIN FETCH member.user
      WHERE c.chatId = :chatId
      """)
  Optional<Chat> findByChatIdWithDetails(@Param("chatId") UUID chatId);

  Optional<Chat> findByImageRef(String imageRef);

  @Query(
      """
      SELECT DISTINCT c FROM Chat c
      LEFT JOIN FETCH c.owner
      LEFT JOIN FETCH c.members member
      LEFT JOIN FETCH member.user
      WHERE c.owner IS NULL
        AND EXISTS (
          SELECT 1 FROM ChatMember m1
          WHERE m1.chat = c AND m1.user.id = :userA
        )
        AND EXISTS (
          SELECT 1 FROM ChatMember m2
          WHERE m2.chat = c AND m2.user.id = :userB
        )
      """)
  List<Chat> findDirectChatsBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);
}

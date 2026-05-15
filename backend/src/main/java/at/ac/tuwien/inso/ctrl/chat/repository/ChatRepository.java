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
      JOIN c.members member
      WHERE member.user = :user
      """)
  List<Chat> findChatsForUser(@Param("user") User user);
}

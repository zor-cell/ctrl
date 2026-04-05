package at.ac.tuwien.inso.ctrl.chat.repository;

import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMemberRepository extends JpaRepository<ChatMember, UUID> {
}

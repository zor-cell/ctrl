package at.ac.tuwien.inso.ctrl.chat.repository;

import at.ac.tuwien.inso.ctrl.chat.entity.Reaction;
import at.ac.tuwien.inso.ctrl.chat.entity.ReactionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionRepository extends JpaRepository<Reaction, ReactionId> {}

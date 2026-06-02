package at.ac.tuwien.inso.ctrl.chat.repository;

import at.ac.tuwien.inso.ctrl.chat.entity.Shared;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedRepository extends JpaRepository<Shared, UUID> {}

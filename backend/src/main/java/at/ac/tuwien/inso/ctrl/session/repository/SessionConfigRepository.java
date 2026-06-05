package at.ac.tuwien.inso.ctrl.session.repository;

import at.ac.tuwien.inso.ctrl.session.entity.SessionConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionConfigRepository extends JpaRepository<SessionConfig, UUID> {
  Optional<SessionConfig> findByUserId(UUID userId);
}

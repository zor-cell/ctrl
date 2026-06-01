package at.ac.tuwien.inso.ctrl.session.repository;

import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<SessionHistory, UUID> {
  Optional<SessionHistory> findTopByUserOrderByStartDesc(User user);
}

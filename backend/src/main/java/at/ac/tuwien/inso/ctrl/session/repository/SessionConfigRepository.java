package at.ac.tuwien.inso.ctrl.session.repository;

import at.ac.tuwien.inso.ctrl.session.entity.SessionConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionConfigRepository extends JpaRepository<SessionConfig, UUID> {
  Optional<SessionConfig> findByUserId(UUID userId);

  @Modifying
  @Query(
      value =
          """
        UPDATE session_config
          SET time_limit_minutes = COALESCE(next_time_limit_minutes, time_limit_minutes),
              post_limit = COALESCE(next_post_limit, post_limit),
              is_forced_cutoff = COALESCE(next_is_forced_cutoff, is_forced_cutoff),
              next_time_limit_minutes = NULL,
              next_post_limit = NULL,
              next_is_forced_cutoff = NULL
          WHERE next_time_limit_minutes IS NOT NULL
             OR next_post_limit IS NOT NULL
             OR next_is_forced_cutoff IS NOT NULL
        """,
      nativeQuery = true)
  void applyNextDayLimits();
}

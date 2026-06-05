package at.ac.tuwien.inso.ctrl.session.repository;

import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.session.entity.DailySessionStatsProjection;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.user.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<SessionHistory, UUID> {
  Optional<SessionHistory> findTopByUserOrderByStartDesc(User user);

  @Query("""
          SELECT
          COALESCE(SUM(s.minutes), 0) AS totalMinutes,
          COALESCE(SUM(s.postsViewed), 0) AS totalPostsViewed
          FROM SessionHistory s
          WHERE s.user = :user AND s.start >= :startOfDay
  """)
  DailySessionStatsProjection getDailyStats(@Param("user") User user, @Param("startOfDay") LocalDateTime startOfDay);
}

package at.ac.tuwien.inso.ctrl.session.repository;

import at.ac.tuwien.inso.ctrl.session.entity.DailySessionStatsProjection;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<SessionHistory, UUID> {
  Optional<SessionHistory> findTopByUserOrderByStartDesc(User user);

  @Query(
      """
              SELECT
              COALESCE(SUM(s.seconds), 0) AS totalSeconds,
              COALESCE(SUM(s.postsViewed), 0) AS totalPostsViewed
              FROM SessionHistory s
              WHERE s.user = :user AND s.start >= :trackingStart
      """)
  DailySessionStatsProjection getDailyStats(
      @Param("user") User user, @Param("trackingStart") LocalDateTime trackingStart);

  @Query(
      value =
          """
          WITH days AS (
            SELECT generate_series(
              DATE_TRUNC('day', CURRENT_TIMESTAMP - INTERVAL '7 days'),
              DATE_TRUNC('day', CURRENT_TIMESTAMP) - INTERVAL '1 day',
              INTERVAL '1 day'
            )::date AS day
          )
          SELECT
            d.day,
            COALESCE(SUM(sh.minutes), 0) AS totalMinutes,
            COUNT(sh.session_id) AS sessionCount
          FROM days d
          LEFT JOIN session_history sh ON DATE(sh.started_at) = d.day
            AND sh.user_id = :userId
            AND sh.minutes IS NOT NULL
          GROUP BY d.day
          ORDER BY d.day
          """,
      nativeQuery = true)
  List<Object[]> findLastSevenDaysSummary(@Param("userId") UUID userId);

  @Query(
      value =
          """
          WITH date_range AS (
            SELECT generate_series(
              DATE_TRUNC('day', CURRENT_TIMESTAMP - INTERVAL '60 days'),
              DATE_TRUNC('day', CURRENT_TIMESTAMP) - INTERVAL '1 day',
              INTERVAL '1 day'
            )::date AS day
          ),
          daily_totals AS (
            SELECT
              DATE(started_at) AS day,
              COALESCE(SUM(minutes), 0) AS totalMinutes,
              COUNT(*) AS sessionCount
            FROM session_history
            WHERE user_id = :userId
              AND started_at >= DATE_TRUNC('day', CURRENT_TIMESTAMP - INTERVAL '60 days')
              AND started_at < DATE_TRUNC('day', CURRENT_TIMESTAMP)
              AND minutes IS NOT NULL
            GROUP BY DATE(started_at)
          )
          SELECT
            EXTRACT(ISODOW FROM dr.day)::int AS dayOfWeek,
            COALESCE(AVG(dt.totalMinutes), 0) AS avgTotalMinutes,
            COALESCE(AVG(dt.sessionCount), 0) AS avgSessionCount,
            COUNT(dr.day)::int AS daysInPeriod
          FROM date_range dr
          LEFT JOIN daily_totals dt ON dr.day = dt.day
          GROUP BY EXTRACT(ISODOW FROM dr.day)
          ORDER BY EXTRACT(ISODOW FROM dr.day)
          """,
      nativeQuery = true)
  List<Object[]> findSixtyDayDayOfWeekAverages(@Param("userId") UUID userId);

  @Query(
      """
      SELECT s FROM SessionHistory s
      WHERE s.user = :user
        AND s.start >= :dayStart
        AND s.start < :dayEnd
        AND s.minutes IS NOT NULL
      ORDER BY s.start
      """)
  List<SessionHistory> findSessionsForDay(
      @Param("user") User user,
      @Param("dayStart") LocalDateTime dayStart,
      @Param("dayEnd") LocalDateTime dayEnd);

  @Query(
      value =
          """
          SELECT * FROM session_history
          WHERE user_id = :userId
            AND EXTRACT(ISODOW FROM started_at) = :dayOfWeek
            AND started_at >= DATE_TRUNC('day', CURRENT_TIMESTAMP - INTERVAL '60 days')
            AND started_at < DATE_TRUNC('day', CURRENT_TIMESTAMP)
            AND minutes IS NOT NULL
          ORDER BY started_at
          """,
      nativeQuery = true)
  List<SessionHistory> findSessionsForDayOfWeekInPeriod(
      @Param("userId") UUID userId, @Param("dayOfWeek") int dayOfWeek);
}

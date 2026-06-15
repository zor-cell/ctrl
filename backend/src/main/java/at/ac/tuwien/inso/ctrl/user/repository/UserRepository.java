package at.ac.tuwien.inso.ctrl.user.repository;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByUsername(String username);

  @Query(
      """
      SELECT u FROM User u
      WHERE u.username = :username
        AND (u.deleted = false OR u.deleted IS NULL)
      """)
  Optional<User> findByUsernameAndDeletedFalse(@Param("username") String username);

  @Query(
      """
      SELECT u FROM User u
      WHERE u.id = :id
        AND (u.deleted = false OR u.deleted IS NULL)
      """)
  Optional<User> findByIdAndDeletedFalse(@Param("id") UUID id);

  @Query(
      """
      SELECT u FROM User u
      WHERE u.isSuspended = false
      AND (u.deleted = false OR u.deleted IS NULL)
      AND LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
      """)
  List<User> findByUsernameContainingIgnoreCaseAndFilterSuspended(String search);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Modifying
  @Query(
      value = "UPDATE users SET is_verified = false WHERE is_verified IS NULL",
      nativeQuery = true)
  int backfillNullVerifiedUsers();

  // query to delete orphan users. this means after a day of the email-address not
  // being registered => Delete user
  @Modifying
  @Query(
      value =
          "DELETE FROM users WHERE is_email_verified = false"
              + " AND created_at < NOW() - "
              + "INTERVAL '1 day'",
      nativeQuery = true)
  void deleteUnverifiedUsersOlderThanOneDay();

  Optional<User> findByEmail(String email);

  @Query(
      value =
          "SELECT * FROM users WHERE is_email_verified = false"
              + " AND created_at < NOW() - INTERVAL '1 day'",
      nativeQuery = true)
  List<User> findEmailUnverifiedUsersOlderThanOneDay();

  /** Calculates the average vector of the user's last 50 upvoted posts. */
  @Query(
      value =
          """
          SELECT UNNEST(CAST(AVG(sub.weighted_embedding) AS real[]))
          FROM (
              SELECT
                ARRAY(
                    SELECT elem * CAST(
                        CASE
                            -- Explicit downvote [-0.5]
                            WHEN pv.vote_type = 'down' THEN -0.5
                            -- Explicit upvote [1.0 to 2.0]
                            WHEN pv.vote_type = 'up' THEN 1.0 + LEAST(COALESCE(sp.total_duration_ms, 0) / 30000.0, 1.0)
                            -- Implicit retention: [0 to 0.5]
                            ELSE LEAST(COALESCE(sp.total_duration_ms, 0) / 30000.0, 1.0) * 0.5
                        END
                    AS float8)
                    FROM UNNEST(p.embedding::real[]) AS elem
                )::vector AS weighted_embedding
              FROM seen_posts sp
              JOIN posts p ON sp.post_id = p.post_id
              LEFT JOIN post_votes pv ON sp.post_id = pv.post_id AND CAST(sp.user_id AS varchar) = pv.user_id
              WHERE sp.user_id = :userId
                AND p.embedding IS NOT NULL
                AND (pv.vote_type IS NOT NULL OR sp.total_duration_ms > 1500)
              ORDER BY sp.last_seen_at DESC
              LIMIT 500
          ) sub
          """,
      nativeQuery = true)
  List<Float> calculateUserInteractionPreferenceVector(@Param("userId") UUID userId);

  User getUserById(UUID uuid);
}

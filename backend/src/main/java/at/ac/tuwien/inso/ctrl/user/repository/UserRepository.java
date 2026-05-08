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

  List<User> findByUsernameContainingIgnoreCase(String search);

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
          "DELETE FROM users WHERE is_verified = false"
              + " AND created_at < NOW() - "
              + "INTERVAL '1 day'",
      nativeQuery = true)
  void deleteUnverifiedUsersOlderThanOneDay();

  Optional<User> findByEmail(String email);

  /** Calculates the average vector of the user's last 50 upvoted posts. */
  @Query(
          value =
                  """
                          SELECT unnest(CAST(avg(sub.embedding) AS real[]))
                          FROM (
                              SELECT p.embedding
                              FROM post_votes pv
                              JOIN posts p ON pv.post_id = p.post_id
                              WHERE pv.user_id = :userId
                                AND pv.vote_type = 'up'
                                AND p.embedding IS NOT NULL
                              ORDER BY pv.created_at DESC
                              LIMIT 50
                          ) sub
                          """,
          nativeQuery = true)
  List<Float> calculateUserInteractionPreferenceVector(@Param("userId") UUID userId);
}

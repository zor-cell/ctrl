package at.ac.tuwien.inso.ctrl.post.repository;

import at.ac.tuwien.inso.ctrl.post.entity.SeenPost;
import at.ac.tuwien.inso.ctrl.post.entity.SeenPostId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeenPostRepository extends JpaRepository<SeenPost, SeenPostId> {

  @Modifying
  @Query(
      value =
          """
    INSERT INTO seen_posts (user_id, post_id, first_seen_at, last_seen_at, total_duration_ms, view_count)
    VALUES (:userId, :postId, NOW(), NOW(), :durationMs, 1)
    ON CONFLICT (user_id, post_id) DO UPDATE SET
        last_seen_at      = NOW(),
        total_duration_ms = seen_posts.total_duration_ms + EXCLUDED.total_duration_ms,
        view_count        = seen_posts.view_count + 1
    """,
      nativeQuery = true)
  void upsertView(
      @Param("userId") UUID userId,
      @Param("postId") String postId,
      @Param("durationMs") long durationMs);

  @Modifying
  @Query("DELETE FROM SeenPost sp WHERE sp.userId = :userId")
  int deleteAllByUserId(@Param("userId") UUID userId);
}

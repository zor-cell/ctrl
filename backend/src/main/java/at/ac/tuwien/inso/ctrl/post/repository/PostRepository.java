package at.ac.tuwien.inso.ctrl.post.repository;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, String> {
  boolean existsByExternalPostId(String externalPostId);

  List<Post> findAllByUserUsernameOrderByCreatedAtDesc(String username);

  Optional<Post> findByPostIdAndUserUsername(String postId, String username);

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
  List<Float> calculateUserPreferenceVector(@Param("userId") String userId);

  /**
   * A query that finds the best matching posts when given multiple weights of preference. PgVector
   * operations can be read about here: <a href="https://github.com/pgvector/pgvector</a>
   */
  @Query(
      value =
          """
                  SELECT p.*
                  FROM posts p
                  LEFT JOIN sources s ON p.source_id = s.source_id
                  LEFT JOIN follow_relations f
                                 ON f.followed_id = p.user_id
                                AND f.follower_id = CAST(:userId AS uuid)
                  WHERE p.is_public = true AND p.embedding IS NOT NULL
                  ORDER BY (
                      -- 1. Topic Interest (Vector Cosine Similarity)
                      (:wTopic * (1 - (p.embedding <=> CAST(:userVector AS real[])::vector)))
                      -- 2. Recency (Decays over time. Halves every ~7 days)
                      + (:wRecency * EXP(-0.1 * EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 86400))
                      -- 3. Trending (Logarithmic scale of post upvotes)
                      + (:wTrending * LN(1 + GREATEST(p.upvotes, 0)))
                      -- 4. Source Quality (Logarithmic scale of source upvotes)
                      + (:wSource * LN(1 + GREATEST(s.upvotes, 0)))
                      -- 5. Exploration (Uncomment and adjust join once follower logic exists)
                      + (:wExploration * CASE WHEN f.follower_id IS NULL AND p.user_id != CAST(:userId AS uuid) THEN 1 ELSE 0 END)
                      ) DESC
                  LIMIT :limit OFFSET :offset
                  """,
      nativeQuery = true)
  List<Post> findPersonalizedFeed(
      @Param("userId") String userId,
      @Param("userVector") float[] userVector,
      @Param("wTopic") double wTopic,
      @Param("wRecency") double wRecency,
      @Param("wTrending") double wTrending,
      @Param("wSource") double wSource,
      @Param("wExploration") double wExploration,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
  Page<Post> findAllPaginated(Pageable pageable);

  @Query(
      """
    SELECT DISTINCT p FROM Post p
    LEFT JOIN FETCH p.user
    LEFT JOIN FETCH p.externalAuthor
    LEFT JOIN FETCH p.postLabels pl LEFT JOIN FETCH pl.label
    LEFT JOIN FETCH p.postTopics pt LEFT JOIN FETCH pt.topic
    LEFT JOIN FETCH p.source s LEFT JOIN FETCH s.singleSources
    LEFT JOIN FETCH p.comments c LEFT JOIN FETCH c.user
    WHERE p.postId IN :postIds
    """)
  List<Post> findAllWithAssociationsByIds(@Param("postIds") List<String> postIds);
}

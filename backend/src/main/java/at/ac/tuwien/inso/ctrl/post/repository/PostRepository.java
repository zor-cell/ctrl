package at.ac.tuwien.inso.ctrl.post.repository;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, String> {
  boolean existsByExternalPostId(String externalPostId);

  List<Post> findAllByUserUsernameOrderByCreatedAtDesc(String username);

  Optional<Post> findByPostIdAndUserUsername(String postId, String username);

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
                                AND f.follower_id = :userId
                  -- Count how many topic tags overlap
                  LEFT JOIN (
                                SELECT post_id, COUNT(*) as overlap_count
                                FROM post_topics
                                WHERE topic_id IN (:topicIds)
                                GROUP BY post_id
                            ) topic_overlap ON topic_overlap.post_id = p.post_id
                  WHERE p.is_public = true AND p.embedding IS NOT NULL
                  ORDER BY (
                      -- User interaction preference (Vector Cosine Similarity) [-1 to 1]
                      (:wInteraction * (1 - (p.embedding <=> CAST(:interactionVector AS real[])::vector)))
                      -- Topic preference (Vector Cosine Similarity) [-1 to 1], Exact topic overlap [0 to 1]
                      + (:wTopic * (1 - ((p.embedding <=> CAST(:topicVector AS real[])::vector)) + (COALESCE(topic_overlap.overlap_count, 0) / 10.0)))
                      -- Recency (Exponential decay, halves every ~2.5 days) [0 to 1]
                      + (:wRecency * EXP(-0.3 * EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 86400))
                      -- Trending (Logarithmic scale of post upvotes)
                      + (:wTrending * LN(1 + p.upvotes))
                      -- Source Quality (Logarithmic scale of source upvotes)
                      + (:wSource * LN(1 + s.upvotes))
                      -- Exploration
                      + (:wExploration * CASE WHEN f.follower_id IS NULL AND p.user_id != :userId THEN 1 ELSE 0 END)
                      ) DESC
                  LIMIT :limit OFFSET :offset
                  """,
      nativeQuery = true)
  List<Post> findPersonalizedFeed(
      @Param("userId") UUID userId,
      @Param("wInteraction") double wInteraction,
      @Param("interactionVector") float[] interactionVector,
      @Param("wTopic") double wTopic,
      @Param("topicVector") float[] topicVector,
      @Param("topicIds") List<String> topicIds,
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

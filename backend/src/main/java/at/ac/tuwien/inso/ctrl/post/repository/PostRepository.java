package at.ac.tuwien.inso.ctrl.post.repository;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import at.ac.tuwien.inso.ctrl.post.entity.PostScoreProjection;
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
                  WITH computed_scores AS (
                      SELECT p.*,
                             -- User interaction preference (Vector Cosine Similarity) [-1 to 1]
                             CASE
                               WHEN CAST(:interactionVector AS real[]) IS NULL THEN 0
                               ELSE (:wInteraction * (1 - (p.embedding <=> CAST(:interactionVector AS real[])::vector)))
                             END AS score_interaction,
                             (
                               -- Topic preference (Vector Cosine Similarity) [-1 to 1]
                               CASE
                                 WHEN CAST(:topicVector AS real[]) IS NULL THEN 0
                                 ELSE (:wTopic * (1 - (p.embedding <=> CAST(:topicVector AS real[])::vector)))
                               END
                              -- Exact topic overlap [0 to 1]
                               + :wTopic * (COALESCE((SELECT COUNT(*) FROM post_topics pt WHERE pt.post_id = p.post_id AND pt.topic_id IN (:topicIds)), 0) / 10.0)
                             ) AS score_topic,
                             -- Recency (Exponential decay, halves every ~2.5 days) [0 to 1]
                             (:wRecency * EXP(-0.3 * EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 86400)) AS score_recency,
                             -- Trending (Wilson score post upvotes / downvotes) [0 to 1] TODO is currently more like popularity than trending
                             (:wTrending * wilson_score(
                               COALESCE((SELECT COUNT(*) FROM post_votes pv WHERE pv.post_id = p.post_id AND pv.vote_type = 'up'), 0)::integer,
                               COALESCE((SELECT COUNT(*) FROM post_votes pv WHERE pv.post_id = p.post_id AND pv.vote_type = 'down'), 0)::integer
                             )) AS score_trending,
                             -- Source Quality (Wilson score of source upvotes / downvotes) [0 to 1]
                             (:wSource * wilson_score(s.upvotes, s.downvotes)) AS score_source,
                             -- Exploration [0 or 1]
                             (:wExploration * CASE WHEN f.follower_id IS NULL AND p.user_id != :userId THEN 1 ELSE 0 END) AS score_exploration
                      FROM posts p
                      LEFT JOIN sources s ON p.source_id = s.source_id
                      LEFT JOIN follow_relations f
                                     ON f.followed_id = p.user_id
                                    AND f.follower_id = :userId
                      WHERE p.is_public = true
                            AND p.embedding IS NOT NULL
                            AND NOT EXISTS (
                               SELECT 1 FROM seen_posts sp
                               WHERE sp.user_id = :userId
                                 AND sp.post_id = p.post_id
                            )
                  )
                  SELECT
                      c.post_id AS postId,
                      c.score_interaction AS scoreInteraction,
                      c.score_topic AS scoreTopic,
                      c.score_recency AS scoreRecency,
                      c.score_trending AS scoreTrending,
                      c.score_source AS scoreSource,
                      c.score_exploration AS scoreExploration,
                      (c.score_interaction + c.score_topic + c.score_recency + c.score_trending + c.score_source + c.score_exploration) AS totalScore
                  FROM computed_scores c
                  ORDER BY totalScore DESC
                  LIMIT :limit OFFSET :offset
                  """,
      nativeQuery = true)
  List<PostScoreProjection> findPersonalizedFeed(
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

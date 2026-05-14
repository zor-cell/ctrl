package at.ac.tuwien.inso.ctrl.search.repository;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostScoreProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchRepository extends JpaRepository<Post, String> {

  /**
   * Searches for posts based on query embedding similarity with optional topic filtering. Uses a
   * candidate pool approach where top N most similar posts are selected by cosine similarity, then
   * algorithmic scoring is applied to rank the results.
   *
   * @param userId Current user ID for seen posts exclusion and personalization
   * @param queryEmbedding The embedded search query vector
   * @param topicIds Optional topic IDs to filter by (OR logic)
   * @param wInteraction Weight for interaction preference score
   * @param interactionVector User's interaction preference vector
   * @param wTopic Weight for topic preference score
   * @param topicVector User's topic preference vector
   * @param wRecency Weight for recency score
   * @param wTrending Weight for trending score
   * @param wSource Weight for source quality score
   * @param wExploration Weight for exploration score
   * @param limit Number of results to return
   * @param offset Pagination offset
   * @return List of scored post projections
   */
  @Query(
      value =
          """
          WITH topic_filtered AS (
              -- Step 1: Optional topic filter (OR logic - match ANY topic)
              SELECT DISTINCT p.post_id, p.embedding, p.upvotes, p.downvotes,
                     p.caption, p.created_at, p.is_public, p.media_ref,
                     p.user_id, p.source_id, p.external_post_id, p.external_author_id
              FROM posts p
              LEFT JOIN post_topics pt ON p.post_id = pt.post_id
              WHERE p.embedding IS NOT NULL
                AND p.is_public = true
                AND NOT EXISTS (
                    SELECT 1 FROM seen_posts sp
                    WHERE sp.user_id = CAST(:userId AS uuid)
                      AND sp.post_id = p.post_id
                )
                AND (
                    CAST(:topicIds AS text[]) IS NULL
                    OR CARDINALITY(CAST(:topicIds AS text[])) = 0
                    OR pt.topic_id = ANY(CAST(:topicIds AS text[]))
                )
          ),
          candidate_pool AS (
              -- Step 2: Get most similar posts by cosine distance using pgvector operator
              SELECT tf.post_id,
                     tf.embedding,
                     tf.upvotes,
                     tf.downvotes,
                     tf.caption,
                     tf.created_at,
                     tf.is_public,
                     tf.media_ref,
                     tf.user_id,
                     tf.source_id,
                     tf.external_post_id,
                     tf.external_author_id,
                     tf.embedding <=> CAST(:queryEmbedding AS vector(1536)) AS distance
              FROM topic_filtered tf
              ORDER BY tf.embedding <=> CAST(:queryEmbedding AS vector(1536))
              LIMIT 200
          ),
          scored_posts AS (
              -- Step 3: Apply algorithmic scoring to candidate pool
              SELECT cp.post_id,
                     cp.distance,
                     cp.upvotes,
                     cp.downvotes,
                     cp.caption,
                     cp.created_at,
                     cp.is_public,
                     cp.media_ref,
                     cp.user_id,
                     cp.source_id,
                     cp.external_post_id,
                     cp.external_author_id,
                     -- User interaction preference (Vector Cosine Similarity) [-1 to 1]
                     CASE
                       WHEN CAST(:interactionVector AS real[]) IS NULL THEN 0.0
                       ELSE (:wInteraction * (1 - (cp.embedding <=> CAST(:interactionVector AS real[])::vector)))
                     END AS score_interaction,
                     -- Topic preference (Vector Cosine Similarity + Exact overlap) [-1 to 1]
                     (:wTopic * (
                       -- Vector similarity component (60%)
                       CASE
                         WHEN CAST(:topicVector AS real[]) IS NULL THEN 0.0
                         ELSE (0.6 * (1 - (cp.embedding <=> CAST(:topicVector AS real[])::vector)))
                       END
                       -- Exact topic overlap component (40%)
                       + CASE
                         WHEN CAST(:topicIds AS text[]) IS NULL OR CARDINALITY(CAST(:topicIds AS text[])) = 0 THEN 0.0
                         ELSE (0.4 * (
                           SELECT COUNT(*)::float
                           FROM post_topics pt2
                           WHERE pt2.post_id = cp.post_id
                             AND pt2.topic_id = ANY(CAST(:topicIds AS text[]))
                         ) / CARDINALITY(CAST(:topicIds AS text[])))
                       END
                     )) AS score_topic,
                     -- Recency (Exponential decay, halves every ~2.5 days) [0 to 1]
                     (:wRecency * EXP(GREATEST(-0.3 * EXTRACT(EPOCH FROM (NOW() - cp.created_at)) / 86400, -700))) AS score_recency,
                     -- Trending (Wilson score of post upvotes / downvotes) [0 to 1]
                     (:wTrending * wilson_score(
                        COALESCE(cp.upvotes, 0)::integer,
                        COALESCE(cp.downvotes, 0)::integer
                     )) AS score_trending,
                     -- Source Quality (Wilson score of source upvotes / downvotes) [0 to 1]
                     (:wSource * wilson_score(
                        COALESCE(s.upvotes, 0)::integer,
                        COALESCE(s.downvotes, 0)::integer
                     )) AS score_source,
                     -- Exploration [0 or 1] - boosts posts from non-followed users
                     (:wExploration * CASE WHEN f.follower_id IS NULL AND cp.user_id != CAST(:userId AS uuid) THEN 1 ELSE 0 END) AS score_exploration
              FROM candidate_pool cp
              LEFT JOIN sources s ON cp.source_id = s.source_id
              LEFT JOIN follow_relations f
                     ON f.followed_id = cp.user_id
                    AND f.follower_id = CAST(:userId AS uuid)
          )
          SELECT
              sp.post_id AS postId,
              sp.score_interaction AS scoreInteraction,
              sp.score_topic AS scoreTopic,
              sp.score_recency AS scoreRecency,
              sp.score_trending AS scoreTrending,
              sp.score_source AS scoreSource,
              sp.score_exploration AS scoreExploration,
              (sp.score_interaction + sp.score_topic + sp.score_recency + sp.score_trending + sp.score_source + sp.score_exploration) AS totalScore
          FROM scored_posts sp
          ORDER BY (sp.score_interaction + sp.score_topic + sp.score_recency + sp.score_trending + sp.score_source + sp.score_exploration) DESC
          LIMIT :limit OFFSET :offset
          """,
      nativeQuery = true)
  List<PostScoreProjection> searchPostsByEmbedding(
      @Param("userId") UUID userId,
      @Param("queryEmbedding") float[] queryEmbedding,
      @Param("topicIds") String[] topicIds,
      @Param("wInteraction") double wInteraction,
      @Param("interactionVector") float[] interactionVector,
      @Param("wTopic") double wTopic,
      @Param("topicVector") float[] topicVector,
      @Param("wRecency") double wRecency,
      @Param("wTrending") double wTrending,
      @Param("wSource") double wSource,
      @Param("wExploration") double wExploration,
      @Param("limit") int limit,
      @Param("offset") int offset);
}

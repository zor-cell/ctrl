package at.ac.tuwien.inso.ctrl.post.repository;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostScoreProjection;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
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

  List<Post> findAllByUserUsernameAndIsPublicTrueOrderByCreatedAtDesc(String username);

  Optional<Post> findByPostIdAndUserUsername(String postId, String username);

  boolean existsByMediaRef(String mediaRef);

  @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.mediaRef = :mediaRef")
  Optional<Post> findByMediaRefWithUser(@Param("mediaRef") String mediaRef);

  @Query(
      """
      SELECT p FROM Post p
      JOIN FETCH p.user
      WHERE p.mediaRef = :mediaRef
         OR p.mediaRef LIKE CONCAT('%', :mediaRef)
      """)
  List<Post> findAllByMediaRefReferenceWithUser(@Param("mediaRef") String mediaRef);

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
                             (:wTopic * (
                               -- Topic preference (Vector Cosine Similarity) [-1 to 1] * 0.6
                               CASE
                                 WHEN CAST(:topicVector AS real[]) IS NULL THEN 0
                                 ELSE (0.6 * (1 - (p.embedding <=> CAST(:topicVector AS real[])::vector)))
                               END
                               -- Exact topic overlap [0 to 1] * 0.4
                               + CASE
                                 WHEN :topicIds IS NULL OR CARDINALITY(ARRAY[:topicIds]) = 0 THEN 0
                                 ELSE (0.4 * (
                                   SELECT COUNT(*)::float
                                   FROM post_topics pt
                                   WHERE pt.post_id = p.post_id AND pt.topic_id IN (:topicIds)
                                 ) / CARDINALITY(ARRAY[:topicIds]))
                               END
                             )) AS score_topic,
                             -- Recency (Exponential decay, halves every ~2.5 days) [0 to 1]
                             (:wRecency * EXP(GREATEST(-0.3 * EXTRACT(EPOCH FROM (NOW() - p.created_at)) / 86400, -700))) AS score_recency,
                             -- Popularity (Wilson score post upvotes / downvotes) [0 to 1]
                             (:wPopularity * wilson_score(
                                COALESCE(p.upvotes, 0)::integer,
                                COALESCE(p.downvotes, 0)::integer
                             )) AS score_popularity,
                             -- Source Quality (Wilson score of source upvotes / downvotes) [0 to 1]
                             (:wSource * wilson_score(
                                COALESCE(s.upvotes, 0)::integer,
                                COALESCE(s.downvotes, 0)::integer
                             )) AS score_source,
                             -- Exploration [0 to 1]
                            (
                             CASE
                               -- Slider favors Exploration (> 0.5): Boost unfollowed users
                               WHEN :wExploration > 0.5 AND fr.friend_id_1 IS NULL AND f.follower_id IS NULL AND (p.user_id IS NULL OR p.user_id != :userId)
                                 THEN (:wExploration - 0.5) * 2.0
                               -- Slider favors Following (< 0.5): Boost friend users
                               WHEN :wExploration < 0.5 AND fr.friend_id_1 IS NOT NULL
                                 THEN (0.5 - :wExploration) * 2.0
                               -- Slider favors Following (< 0.5): Boost followed users
                               WHEN :wExploration < 0.5 AND f.follower_id IS NOT NULL
                                 THEN (0.5 - :wExploration) * 1.5
                               ELSE 0.0
                             END
                             ) AS score_exploration
                      FROM posts p
                      LEFT JOIN sources s ON p.source_id = s.source_id
                      LEFT JOIN follow_relations f
                         ON f.followed_id = p.user_id
                         AND f.follower_id = :userId
                      LEFT JOIN friend_relation fr
                         ON (fr.friend_id_1 = :userId AND fr.friend_id_2 = p.user_id)
                         OR (fr.friend_id_1 = p.user_id AND fr.friend_id_2 = :userId)
                      WHERE p.post_id IN (:candidatePosts)
                  )
                  SELECT
                      c.post_id AS postId,
                      c.score_interaction AS scoreInteraction,
                      c.score_topic AS scoreTopic,
                      c.score_recency AS scoreRecency,
                      c.score_popularity AS scoreTrending,
                      c.score_source AS scoreSource,
                      c.score_exploration AS scoreExploration,
                      (c.score_interaction + c.score_topic + c.score_recency + c.score_popularity + c.score_source + c.score_exploration) AS totalScore
                  FROM computed_scores c
                  ORDER BY totalScore DESC
                  LIMIT :limit OFFSET :offset
                  """,
      nativeQuery = true)
  List<PostScoreProjection> getRankedPosts(
      @Param("candidatePosts") List<String> candidatePosts,
      @Param("userId") UUID userId,
      @Param("wInteraction") double wInteraction,
      @Param("interactionVector") float[] interactionVector,
      @Param("wTopic") double wTopic,
      @Param("topicVector") float[] topicVector,
      @Param("topicIds") List<String> topicIds,
      @Param("wRecency") double wRecency,
      @Param("wPopularity") double wPopularity,
      @Param("wSource") double wSource,
      @Param("wExploration") double wExploration,
      @Param("limit") int limit,
      @Param("offset") int offset);

  /**
   * A query that populates a candidate pool with indexed queries so ordering is very efficient.
   * Retrieves posts that are likely good in different metrics and returns their ids
   */
  @Query(
      value =
          """
    WITH public_posts AS (
            SELECT p.post_id, p.embedding, p.created_at, p.upvotes
            FROM posts p
            WHERE p.is_public = true
                AND NOT EXISTS (SELECT 1 FROM seen_posts sp WHERE sp.user_id = :userId AND sp.post_id = p.post_id)
                AND (p.user_id IS NULL OR p.user_id != :userId)
    )
    ( --interaction candidates
      SELECT pp.post_id FROM public_posts pp
       WHERE pp.embedding IS NOT NULL AND CAST(:interactionVector AS real[]) IS NOT NULL
       ORDER BY pp.embedding <=> CAST(:interactionVector AS real[])::vector LIMIT :poolSize)
    UNION
    ( --topic candidates
        SELECT pp.post_id FROM public_posts pp
         WHERE pp.embedding IS NOT NULL AND CAST(:topicVector AS real[]) IS NOT NULL
         ORDER BY pp.embedding <=> CAST(:topicVector AS real[])::vector LIMIT :poolSize)
    UNION
    ( --recency candidates
        SELECT pp.post_id FROM public_posts pp
         ORDER BY pp.created_at DESC LIMIT :poolSize)
    UNION
    ( --popularity candidates
        SELECT pp.post_id FROM public_posts pp
         ORDER BY pp.upvotes DESC LIMIT :poolSize)
    """,
      nativeQuery = true)
  List<String> getPublicPostCandidatePool(
      @Param("userId") UUID userId,
      @Param("interactionVector") float[] interactionVector,
      @Param("topicVector") float[] topicVector,
      @Param("poolSize") int poolSize);

  @Query(
      value =
          """
        WITH private_posts AS (
            SELECT p.post_id, p.embedding, p.created_at, p.upvotes
            FROM posts p
            WHERE p.is_public = false
              AND EXISTS (
                 SELECT 1 FROM friend_relation f
                 WHERE (f.friend_id_1 = :userId AND f.friend_id_2 = p.user_id)
                    OR (f.friend_id_1 = p.user_id AND f.friend_id_2 = :userId)
              )
              AND (
                (:fetchOld = false AND NOT EXISTS (SELECT 1 FROM seen_posts sp WHERE sp.user_id = :userId AND sp.post_id = p.post_id))
                OR
                (:fetchOld = true AND EXISTS (SELECT 1 FROM seen_posts sp WHERE sp.user_id = :userId AND sp.post_id = p.post_id))
              )
        )
        (--interaction candidates
            SELECT pp.post_id FROM private_posts pp
            WHERE pp.embedding IS NOT NULL AND CAST(:interactionVector AS real[]) IS NOT NULL
            ORDER BY pp.embedding <=> CAST(:interactionVector AS real[])::vector LIMIT :poolSize )
          UNION
          ( --topic candidates
            SELECT pp.post_id FROM private_posts pp
            WHERE pp.embedding IS NOT NULL AND CAST(:topicVector AS real[]) IS NOT NULL
            ORDER BY pp.embedding <=> CAST(:topicVector AS real[])::vector LIMIT :poolSize )
          UNION
          ( --recency candidates
            SELECT pp.post_id FROM private_posts pp
            ORDER BY pp.created_at DESC LIMIT :poolSize )
          UNION
          ( --popularity candidates
            SELECT pp.post_id FROM private_posts pp
            ORDER BY pp.upvotes DESC LIMIT :poolSize )
    """,
      nativeQuery = true)
  List<String> getPrivatePostCandidatePool(
      @Param("userId") UUID userId,
      @Param("interactionVector") float[] interactionVector,
      @Param("topicVector") float[] topicVector,
      @Param("poolSize") int poolSize,
      @Param("fetchOld") boolean fetchOld);

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

  @Query(
      value =
          """
        SELECT p.* FROM posts p
        WHERE NOT EXISTS (
            SELECT 1 FROM post_topics pt WHERE pt.post_id = p.post_id
        )
        AND p.embedding IS NOT NULL
        LIMIT :limit
        OFFSET :offset
        """,
      nativeQuery = true)
  List<Post> findPostsWithoutTopics(@Param("limit") int limit, @Param("offset") int offset);

  @Query(
      value =
          """
        SELECT COUNT(*) FROM posts p
        WHERE NOT EXISTS (
            SELECT 1 FROM post_topics pt WHERE pt.post_id = p.post_id
        )
        AND p.embedding IS NOT NULL
        """,
      nativeQuery = true)
  long countPostsWithoutTopics();

  Optional<Post> findBySource(Source source);

  Optional<Post> getPostByPostId(String contentId);
}

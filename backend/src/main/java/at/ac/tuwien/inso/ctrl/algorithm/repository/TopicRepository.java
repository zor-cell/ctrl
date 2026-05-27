package at.ac.tuwien.inso.ctrl.algorithm.repository;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, String> {

  boolean existsByName(String name);

  Optional<Topic> findByNameIgnoreCase(String name);

  @Query(
          value =
                  """
                  SELECT UNNEST(CAST(AVG(t.embedding) AS real[]))
                  FROM topics t
                  WHERE t.topic_id IN (:topicIds)
                    AND t.embedding IS NOT NULL
                  """,
          nativeQuery = true)
  List<Float> calculateTopicPreferenceVector(@Param("topicIds") List<String> topicIds);

  /**
   * Returns up to {@code limit} topics whose cosine distance to {@code postEmbedding}
   * is strictly below {@code threshold}, ordered by closeness (best match first).
   *
   * <p>Uses pgvector's {@code <=>} operator (cosine distance, range 0–2):
   * <ul>
   *   <li>0.0 = identical vectors</li>
   *   <li>1.0 = orthogonal (unrelated)</li>
   *   <li>2.0 = opposite</li>
   * </ul>
   *
   * Recommended starting value: {@code threshold = 0.35} (cosine similarity >= 0.65).
   * If too many posts get 0 topics assigned → raise threshold (e.g. 0.45).
   * If assignments feel too loose/inaccurate → lower threshold (e.g. 0.25).
   *
   * <p>May return fewer than {@code limit} results when no topics exceed the
   * similarity cutoff — callers must handle an empty list gracefully.
   */
  @Query(
          value =
                  """
                  SELECT t.*
                  FROM topics t
                  WHERE t.embedding IS NOT NULL
                    AND (t.embedding <=> CAST(:postEmbedding AS vector)) < :threshold
                  ORDER BY t.embedding <=> CAST(:postEmbedding AS vector)
                  LIMIT :limit
                  """,
          nativeQuery = true)
  List<Topic> findMatchingTopicsForEmbedding(
          @Param("postEmbedding") float[] postEmbedding,
          @Param("limit") int limit,
          @Param("threshold") double threshold);
}
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

  @Query(
      value =
          """
                    SELECT t.*
                    FROM topics t
                    WHERE t.embedding IS NOT NULL
                    ORDER BY t.embedding <=> CAST(:postEmbedding AS vector)
                    LIMIT :limit
                    """,
      nativeQuery = true)
  List<Topic> findMatchingTopicsForEmbedding(
      @Param("postEmbedding") float[] postEmbedding, @Param("limit") int limit);
}

package at.ac.tuwien.inso.ctrl.algorithm.entity;

import at.ac.tuwien.inso.ctrl.embedding.config.EmbeddingModelConfig;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "algorithm_config")
public class AlgorithmConfig {
  @Id
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(
      name = "user_id")
  private User user;

  @Column(name = "weight_interaction")
  private Integer weightInteraction;

  @Column(name = "weight_recency")
  private Integer weightRecency;

  @Column(name = "weight_exploration")
  private Integer weightExploration;

  @Column(name = "weight_source_quality")
  private Integer weightSourceQuality;

  @Column(name = "weight_trending")
  private Integer weightPopularity;

  @Column(name = "weight_topic")
  private Integer weightTopic;

  @ManyToMany
  @JoinTable(
          name = "algorithm_config_topics",
          joinColumns = @JoinColumn(name = "algorithm_config_id"),
          inverseJoinColumns = @JoinColumn(name = "topic_id")
  )
  @Builder.Default
  private Set<Topic> topics = new LinkedHashSet<>();

  @JdbcTypeCode(SqlTypes.VECTOR)
  @Column(name = "topic_preference_embedding")
  @Array(length = EmbeddingModelConfig.MODEL_DIMENSIONS)
  private float[] topicPreferenceEmbedding;
}

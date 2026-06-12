package at.ac.tuwien.inso.ctrl.topic.entity;

import at.ac.tuwien.inso.ctrl.embedding.config.EmbeddingModelConfig;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "topics")
public class Topic {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "topic_id")
  private String topicId;

  @Column(nullable = false, unique = true)
  private String name;

  private String emoji;

  @Column(name = "created_at")
  @CreationTimestamp
  private LocalDateTime createdAt;

  @JdbcTypeCode(SqlTypes.VECTOR)
  @Column(name = "embedding")
  @Array(length = EmbeddingModelConfig.MODEL_DIMENSIONS)
  private float[] embedding;
}

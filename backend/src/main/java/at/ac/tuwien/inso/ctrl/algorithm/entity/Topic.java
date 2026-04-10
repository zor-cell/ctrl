package at.ac.tuwien.inso.ctrl.algorithm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "topics")
public class Topic {
  @Id
  @Column(name = "topic_id", nullable = false, unique = true)
  private String topicId;

  @Column(nullable = false)
  private String name;

  private String emoji;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "topic")
  @Builder.Default
  private Set<TopicPriority> topicPriorities = new LinkedHashSet<>();
}

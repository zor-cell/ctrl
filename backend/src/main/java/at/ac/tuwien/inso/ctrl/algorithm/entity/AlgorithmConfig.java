package at.ac.tuwien.inso.ctrl.algorithm.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.*;

import java.util.UUID;
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
@Table(name = "algorithm_config")
public class AlgorithmConfig {
  @Id
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(
      name = "user_id")
  private User user;

  @Column(name = "weight_recency")
  private Integer weightRecency;

  @Column(name = "weight_exploration")
  private Integer weightExploration;

  @Column(name = "weight_source_quality")
  private Integer weightSourceQuality;

  @Column(name = "weight_trending")
  private Integer weightTrending;

  @Column(name = "weight_topic")
  private Integer weightTopic;
}

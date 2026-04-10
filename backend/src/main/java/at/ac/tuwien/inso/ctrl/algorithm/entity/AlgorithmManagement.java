package at.ac.tuwien.inso.ctrl.algorithm.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
@Table(name = "algorithm_management")
public class AlgorithmManagement {
  @Id
  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "user_id",
      referencedColumnName = "id",
      nullable = false,
      insertable = false,
      updatable = false)
  private User user;

  @Column(name = "weight_recency")
  private Integer weightRecency;

  @Column(name = "weight_known_vs_new")
  private Integer weightKnownVsNew;

  @Column(name = "weight_source_quality")
  private Integer weightSourceQuality;

  @Column(name = "weight_trending_boost")
  private Integer weightTrendingBoost;

  @Column(name = "weight_topic_interest")
  private Integer weightTopicInterest;
}

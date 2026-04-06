package at.ac.tuwien.inso.ctrl.algorithm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    @Column(nullable = false, unique = true)
    private String username;

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

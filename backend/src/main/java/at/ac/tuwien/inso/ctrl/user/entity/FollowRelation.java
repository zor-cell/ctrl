package at.ac.tuwien.inso.ctrl.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "follow_relations")
public class FollowRelation {

  @EmbeddedId private FollowRelationId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("followerId")
  @JoinColumn(name = "follower_id", referencedColumnName = "id", nullable = false)
  private User follower;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("followedId")
  @JoinColumn(name = "followed_id", referencedColumnName = "id", nullable = false)
  private User followed;

  @Column(name = "since")
  private LocalDateTime since;
}

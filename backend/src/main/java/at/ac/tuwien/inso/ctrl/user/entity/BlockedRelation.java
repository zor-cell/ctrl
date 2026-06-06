package at.ac.tuwien.inso.ctrl.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "blocked_relation")
public class BlockedRelation {

  @EmbeddedId private BlockedRelationId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("user")
  @JoinColumn(name = "user", referencedColumnName = "id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("blocked_user")
  @JoinColumn(name = "blocked_user", referencedColumnName = "id", nullable = false)
  private User blockedUser;

  @Column(name = "since")
  private LocalDateTime since;
}

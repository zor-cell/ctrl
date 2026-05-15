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
@Table(name = "friend_relation")
public class FriendRelation {

  @EmbeddedId private FriendRelationId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("friendId1")
  @JoinColumn(name = "friend_id_1", referencedColumnName = "id", nullable = false)
  private User friend1;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("friendId2")
  @JoinColumn(name = "friend_id_2", referencedColumnName = "id", nullable = false)
  private User friend2;

  @Column(name = "since")
  private LocalDateTime since;
}

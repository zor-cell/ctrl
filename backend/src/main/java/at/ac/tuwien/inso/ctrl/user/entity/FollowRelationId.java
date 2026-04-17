package at.ac.tuwien.inso.ctrl.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class FollowRelationId implements Serializable {
  @Column(name = "follower_id")
  private UUID followerId;

  @Column(name = "followed_id")
  private UUID followedId;
}

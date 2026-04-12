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
@Table(name = "follow_relations")
public class FollowRelation {

    @EmbeddedId
    private FollowRelationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerUsername")
    @JoinColumn(name = "follower_username", referencedColumnName = "username", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followingUsername")
    @JoinColumn(name = "following_username", referencedColumnName = "username", nullable = false)
    private User following;

    @Column(name = "since")
    private LocalDateTime since;
}

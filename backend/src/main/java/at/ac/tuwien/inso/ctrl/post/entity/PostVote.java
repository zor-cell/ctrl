package at.ac.tuwien.inso.ctrl.post.entity;

import at.ac.tuwien.inso.ctrl.common.domain.VoteEntry;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_votes")
public class PostVote implements VoteEntry {

  @EmbeddedId private PostVoteId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("postId")
  @JoinColumn(name = "post_id")
  private Post post;

  /** {@code "up"} or {@code "down"} */
  @Column(name = "vote_type", nullable = false, length = 4)
  private String voteType;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;
}

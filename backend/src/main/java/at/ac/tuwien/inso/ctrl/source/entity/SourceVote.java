package at.ac.tuwien.inso.ctrl.source.entity;

import at.ac.tuwien.inso.ctrl.common.domain.VoteEntry;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "source_votes")
public class SourceVote implements VoteEntry {

  @EmbeddedId private SourceVoteId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("sourceId")
  @JoinColumn(name = "source_id")
  private Source source;

  /** {@code "up"} or {@code "down"} */
  @Column(name = "vote_type", nullable = false, length = 4)
  private String voteType;
}

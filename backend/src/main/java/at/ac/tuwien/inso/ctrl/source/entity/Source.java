package at.ac.tuwien.inso.ctrl.source.entity;

import at.ac.tuwien.inso.ctrl.common.domain.Votable;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sources")
public class Source implements Votable {

  @Id
  @Column(name = "source_id", nullable = false, unique = true)
  private String sourceId;

  private Integer upvotes;

  private Integer downvotes;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "source", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SingleSource> singleSources;
}
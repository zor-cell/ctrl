package at.ac.tuwien.inso.ctrl.source.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "single_source")
public class SingleSource {
  @Id
  @Column(name = "single_source_id", nullable = false, unique = true)
  private String singleSourceId;

  private String journal;

  private String author;

  private String title;

  private String url;

  private LocalDate date;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_id")
  private Source source;
}

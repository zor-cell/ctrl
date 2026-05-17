package at.ac.tuwien.inso.ctrl.source.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
@Table(name = "single_source")
public class SingleSource {
  @Id
  @Column(name = "single_source_id", nullable = false, unique = true)
  private String singleSourceId;

  @Column(length = 255)
  private String journal;

  @Column(length = 255)
  private String author;

  @Column(length = 500)
  private String title;

  @Column(length = 2048)
  private String url;

  private LocalDate date;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_id")
  private Source source;
}

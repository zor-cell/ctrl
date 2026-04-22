package at.ac.tuwien.inso.ctrl.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "labels")
public class Label {
  @Id
  @Column(name = "label_id", nullable = false, unique = true)
  private String labelId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String emoji;

  @Column(columnDefinition = "TEXT")
  private String explanation;
}

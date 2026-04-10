package at.ac.tuwien.inso.ctrl.settings.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
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
@Table(name = "time_management")
public class TimeManagement {
  @Id
  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "user_id",
      referencedColumnName = "id",
      nullable = false,
      insertable = false,
      updatable = false)
  private User user;

  @Column(name = "is_day_hardcutted")
  private Boolean isDayHardcutted;

  @Column(name = "duration_per_day")
  private Integer durationPerDay;

  @Column(name = "posts_per_day")
  private Integer postsPerDay;
}

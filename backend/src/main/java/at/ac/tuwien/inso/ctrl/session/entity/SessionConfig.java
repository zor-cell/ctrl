package at.ac.tuwien.inso.ctrl.session.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "session_config")
public class SessionConfig {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "time_limit_minutes")
  private Integer timeLimitMinutes;

  @Column(name = "next_time_limit_minutes")
  private Integer nextTimeLimitMinutes;

  @Column(name = "post_limit")
  private Integer postLimit;

  @Column(name = "next_post_limit")
  private Integer nextPostLimit;

  @Column(name = "allow_snooze", nullable = false)
  private boolean allowSnooze;

  @Column(name = "next_allow_snooze")
  private Boolean nextAllowSnooze;

  @Column(name = "last_snooze")
  private LocalDateTime lastSnooze;
}

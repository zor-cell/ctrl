package at.ac.tuwien.inso.ctrl.session.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
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

    @Column(name = "post_limit")
    private Integer postLimit;

    // Enforces either forced or soft cutoff of sessions
    @Column(name = "is_forced_cutoff", nullable = false)
    private boolean isForcedCutoff;
}
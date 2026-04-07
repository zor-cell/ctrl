package at.ac.tuwien.inso.ctrl.settings.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "session_history")
public class SessionHistory {
    @Id
    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username", referencedColumnName = "username", nullable = false)
    private User user;

    @Column(name = "start")
    private LocalDateTime start;

    private Integer minutes;

    @Column(name = "posts_viewed")
    private Integer postsViewed;
}

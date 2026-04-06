package at.ac.tuwien.inso.ctrl.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

    @Column(nullable = false)
    private String username;

    @Column(name = "start")
    private LocalDateTime start;

    private Integer minutes;

    @Column(name = "posts_viewed")
    private Integer postsViewed;
}

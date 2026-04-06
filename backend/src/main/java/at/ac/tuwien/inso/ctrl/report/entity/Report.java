package at.ac.tuwien.inso.ctrl.report.entity;

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
@Table(name = "reports")
public class Report {
    @Id
    @Column(name = "report_id", nullable = false, unique = true)
    private String reportId;

    @Column(nullable = false)
    private String content;

    @Column(name = "from_user", nullable = false)
    private String fromUser;

    @Column(name = "username_reported", nullable = false)
    private String usernameReported;

    @Column(name = "post_id_reported")
    private String postIdReported;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

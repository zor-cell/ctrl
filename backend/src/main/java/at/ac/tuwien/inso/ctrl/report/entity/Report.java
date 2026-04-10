package at.ac.tuwien.inso.ctrl.report.entity;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "reports")
public class Report {
  @Id
  @Column(name = "report_id", nullable = false, unique = true)
  private String reportId;

  @Column(nullable = false)
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_user_id", referencedColumnName = "id", nullable = false)
  private User fromUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_user_id", referencedColumnName = "id", nullable = false)
  private User usernameReported;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id_reported", referencedColumnName = "post_id")
  private Post postReported;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}

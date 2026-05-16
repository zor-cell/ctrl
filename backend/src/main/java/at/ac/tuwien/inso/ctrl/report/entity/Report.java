package at.ac.tuwien.inso.ctrl.report.entity;

import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "report_id", nullable = false, unique = true)
  private String reportId;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_user_id", referencedColumnName = "id", nullable = false)
  private User fromUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_user_id", referencedColumnName = "id")
  private User usernameReported;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_external_author_id", referencedColumnName = "external_author_id")
  private ExternalAuthor externalAuthorReported;

  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", nullable = false)
  private ContentTypeEnum contentType;

  @Column(name = "content_id", nullable = false)
  private String contentId;

  @Column(name = "created_at")
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReportStatus status;
}

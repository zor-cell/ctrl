package at.ac.tuwien.inso.ctrl.report.repository;

import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
  List<Report> findByContentTypeAndStatus(
      ContentTypeEnum contentType, ReportStatus reportStatus, PageRequest pageable);

  List<Report> findByContentType(ContentTypeEnum contentType);

  List<Report> findByStatus(ReportStatus reportStatus, PageRequest pageable);

  List<Report> findAllByUsernameReported(User usernameReported);

  List<Report> findAllByExternalAuthorReported(ExternalAuthor externalAuthorReported);

  Optional<Report> findByReportIdAndStatus(String reportId, ReportStatus status);

  List<Report> findAllByUsernameReportedAndStatus(User user, ReportStatus reportStatus);

  List<Report> findAllByExternalAuthorReportedAndStatus(
      ExternalAuthor externalAuthorReported, ReportStatus reportStatus);

  List<Report> findAllByContentIdAndContentType(String id, ContentTypeEnum type);

  boolean existsReportByFromUserAndContentTypeAndContentId(
      User user, ContentTypeEnum type, String contentId);
}

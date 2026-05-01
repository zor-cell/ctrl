package at.ac.tuwien.inso.ctrl.report.repository;

import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, String> {
  List<Report> findByContentTypeAndStatus(ContentTypeEnum contentType, ReportStatus reportStatus, PageRequest pageable);

  List<Report> findByContentType(ContentTypeEnum contentType);

  List<Report> findByStatus(ReportStatus reportStatus, PageRequest pageable);
}

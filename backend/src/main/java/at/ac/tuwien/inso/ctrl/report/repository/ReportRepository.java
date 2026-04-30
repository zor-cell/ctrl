package at.ac.tuwien.inso.ctrl.report.repository;

import at.ac.tuwien.inso.ctrl.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
}

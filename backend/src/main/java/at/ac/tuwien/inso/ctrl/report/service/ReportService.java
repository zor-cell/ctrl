package at.ac.tuwien.inso.ctrl.report.service;

import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


public interface ReportService {
  void makeReport(CreateReportRequest createReportRequest);
}

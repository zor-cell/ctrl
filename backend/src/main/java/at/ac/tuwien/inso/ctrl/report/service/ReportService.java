package at.ac.tuwien.inso.ctrl.report.service;

import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;


public interface ReportService {
  void makeReport(CreateReportRequest reportRequest, User user);
}

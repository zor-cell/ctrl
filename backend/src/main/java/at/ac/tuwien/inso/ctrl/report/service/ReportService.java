package at.ac.tuwien.inso.ctrl.report.service;

import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.report.dto.ReportSummaryResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedCommentResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedPostResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedSourceResponse;
import at.ac.tuwien.inso.ctrl.report.dto.UpdateReportRequest;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;

public interface ReportService {
  void makeReport(CreateReportRequest reportRequest, User user);

  List<ReportedPostResponse> getReportedPosts(ReportStatus reportStatus, int page, int postPerPage);

  void updateReportStatus(UpdateReportRequest request, String id);
  List<ReportedCommentResponse> getReportedComments(ReportStatus reportStatus, int page, int postsPerPage);
  List<ReportSummaryResponse> getAccountReportSummary(String reportId);

  List<ReportedSourceResponse> getReportedSources(ReportStatus status, int page, int postsPerPage);
}

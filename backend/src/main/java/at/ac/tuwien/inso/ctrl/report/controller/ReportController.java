package at.ac.tuwien.inso.ctrl.report.controller;

import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedPostResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedSourceResponse;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  @PostMapping()
  public void makeReport(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody CreateReportRequest request) {

    reportService.makeReport(request, user);
  }

  @GetMapping("/posts")
  public List<ReportedPostResponse> getReportedPostsWithStatusCreated(
      @RequestParam(defaultValue = "CREATED") ReportStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int postsPerPage

      ) {
    return reportService.getReportedPosts(status, page, postsPerPage);
  }

  @GetMapping("/sources")
  public List<ReportedSourceResponse> getReportedSourcesWithStatusCreated(
      @RequestParam(defaultValue = "CREATED") ReportStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int postsPerPage
  ) {
    return reportService.getReportedSources(status, page, postsPerPage);
  }

  @PutMapping("/{id}")
  public void updateReport(
      @PathVariable String id,
      @Valid @RequestBody ReportStatus status) {
    reportService.updateReportStatus(status, id);
  }
}

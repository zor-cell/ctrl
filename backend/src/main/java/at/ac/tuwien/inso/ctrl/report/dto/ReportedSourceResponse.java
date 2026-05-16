package at.ac.tuwien.inso.ctrl.report.dto;

import at.ac.tuwien.inso.ctrl.post.dto.response.SourceResponse;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import java.time.LocalDateTime;

public record ReportedSourceResponse(
    // Report Info
    String reportId,
    String reportReason,
    String reportDescription,
    String reportedByUsername,
    String reportedUsername,
    ContentTypeEnum contentType,
    ReportStatus status,
    LocalDateTime reportedAt,
    SourceResponse sourceResponse) {}

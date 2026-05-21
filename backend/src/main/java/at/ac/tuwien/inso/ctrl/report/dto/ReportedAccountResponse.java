package at.ac.tuwien.inso.ctrl.report.dto;

import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.user.dto.PublicUserProfileResponse;

import java.time.LocalDateTime;

public record ReportedAccountResponse(
    PublicUserProfileResponse profileResponse,
    String reportId,
    String reportReason,
    String reportDescription,
    String reportedByUsername,
    String reportedUsername,
    ContentTypeEnum contentType,
    ReportStatus status,
    LocalDateTime reportedAt
) {
}

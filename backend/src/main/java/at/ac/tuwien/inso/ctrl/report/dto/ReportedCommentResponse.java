package at.ac.tuwien.inso.ctrl.report.dto;

import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import java.time.LocalDateTime;

public record ReportedCommentResponse(
    CommentResponse comment,
    String reportId,
    String reportReason,
    String reportDescription,
    String reportedByUsername,
    String reportedUsername,
    ContentTypeEnum contentType,
    ReportStatus status,
    LocalDateTime reportedAt) {}

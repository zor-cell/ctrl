package at.ac.tuwien.inso.ctrl.report.dto;

import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;

public record ReportSummaryResponse(
    ContentTypeEnum contentType, String reason, String description) {}

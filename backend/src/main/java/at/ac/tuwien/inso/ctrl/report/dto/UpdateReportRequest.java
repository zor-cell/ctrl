package at.ac.tuwien.inso.ctrl.report.dto;

import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReportRequest(
    @NotNull(message = "Status is required") ReportStatus status,
    @NotNull(message = "Action is required") ReportAction action) {}

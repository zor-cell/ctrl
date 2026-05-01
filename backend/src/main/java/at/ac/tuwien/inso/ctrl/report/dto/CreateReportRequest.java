package at.ac.tuwien.inso.ctrl.report.dto;

import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReportRequest(
    @NotBlank(message = "Reason is required")
    String reason,

    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Content ID is required")
    String contentId,

    @NotNull(message = "Content type is required")
    ContentTypeEnum contentType,

    @NotBlank(message = "Username is required")
    String reportingUser

) {

}
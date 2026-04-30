package at.ac.tuwien.inso.ctrl.report.dto;

import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateReportRequest(
    @NotBlank(message = "Reason is required")
    String reason,

    @NotBlank(message = "Detail is required")
    String description,

    @NotNull(message = "Content ID is required")
    String contentId,

    @NotNull(message = "Content type is required")
    ContentTypeEnum contentType

) {

}
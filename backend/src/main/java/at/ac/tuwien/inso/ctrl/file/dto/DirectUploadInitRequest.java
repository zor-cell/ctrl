package at.ac.tuwien.inso.ctrl.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DirectUploadInitRequest(
    @NotBlank String fileName, @NotBlank String contentType, @Positive long size) {}

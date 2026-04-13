package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
    @NotBlank String currentPassword, @NotBlank String newPassword) {}

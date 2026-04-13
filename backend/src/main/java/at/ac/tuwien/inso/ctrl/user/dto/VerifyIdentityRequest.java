package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyIdentityRequest(@NotBlank String code) {}

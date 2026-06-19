package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUsernameRequest(
    @NotBlank
        @Size(min = 3)
        @Pattern(
            regexp = "^(?=.*[A-Za-z0-9])[A-Za-z0-9_.]+$",
            message =
                "Username must be at least 3 characters, contain at least one letter or number, and may only use letters, numbers, underscores and dots")
        String newUsername) {}

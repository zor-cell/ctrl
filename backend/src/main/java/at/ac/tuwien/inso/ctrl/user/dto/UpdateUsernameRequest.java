package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUsernameRequest(
        @NotBlank
        String newUsername
) {
}

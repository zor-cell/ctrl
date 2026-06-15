package at.ac.tuwien.inso.ctrl.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
    @NotBlank String token,
    @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};:,.?]).+$",
            message =
                "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character of (!@#$%^&*()_+-=[]{};:,.?).")
        @NotBlank
        String password) {}

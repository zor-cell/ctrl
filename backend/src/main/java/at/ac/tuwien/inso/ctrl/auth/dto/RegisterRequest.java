package at.ac.tuwien.inso.ctrl.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
    @NotBlank
        @Size(min = 3)
        @Pattern(
            regexp = "^(?=.*[A-Za-z0-9])[A-Za-z0-9_.]+$",
            message =
                "Username must be at least 3 characters, contain at least one letter or number, and may only use letters, numbers, underscores and dots")
        String username,
    @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};:,.?]).+$",
            message =
                "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character of (!@#$%^&*()_+-=[]{};:,.?).")
        @NotBlank
        String password,
    @Email String email,
    @NotBlank String name,
    @PastOrPresent LocalDate birthdate) {}

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
            regexp = "^[A-Za-z0-9_.]+$",
            message = "Username may only contain letters, numbers, underscores and dots")
        String username,
    @NotBlank String password,
    @Email String email,
    @NotBlank String name,
    @PastOrPresent LocalDate birthdate) {}

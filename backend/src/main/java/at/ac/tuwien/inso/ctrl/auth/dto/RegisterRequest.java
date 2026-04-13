package at.ac.tuwien.inso.ctrl.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank
        String username,
        @NotBlank
        String password,
        @Email
        String email,
        @NotBlank
        String name,
        @PastOrPresent
        LocalDate birthdate
) {
}

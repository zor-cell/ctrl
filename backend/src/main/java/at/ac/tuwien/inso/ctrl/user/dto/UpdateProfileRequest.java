package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank
        String name,
        @Email
        String email,
        @PastOrPresent
        LocalDate birthdate
) {
}

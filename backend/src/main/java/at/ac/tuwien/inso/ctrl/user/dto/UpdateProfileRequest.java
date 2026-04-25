package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
    @NotBlank String name,
    @Email String email,
    @Size(max = 280) String bio,
    @PastOrPresent LocalDate birthdate) {}

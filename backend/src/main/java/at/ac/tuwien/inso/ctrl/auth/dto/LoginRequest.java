package at.ac.tuwien.inso.ctrl.auth.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record LoginRequest(
        @NotBlank
        String username,
        @NotBlank
        String password
) { }

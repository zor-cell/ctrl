package at.ac.tuwien.inso.ctrl.auth.dto;

import java.time.LocalDate;

public record RegisterRequest(
        String username,
        String password,
        String email,
        String name,
        LocalDate birthdate
) {
}

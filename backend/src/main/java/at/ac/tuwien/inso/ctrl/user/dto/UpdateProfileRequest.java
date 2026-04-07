package at.ac.tuwien.inso.ctrl.user.dto;

import java.time.LocalDate;

public record UpdateProfileRequest(
        String name,
        LocalDate birthdate
) {
}

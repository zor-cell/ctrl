package at.ac.tuwien.inso.ctrl.user.dto;

public record UpdatePasswordRequest(
        String currentPassword,
        String newPassword
) {
}

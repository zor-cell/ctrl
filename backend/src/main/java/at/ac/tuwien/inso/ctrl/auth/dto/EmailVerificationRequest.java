package at.ac.tuwien.inso.ctrl.auth.dto;

public record EmailVerificationRequest(
    String username,
    String code
) { }

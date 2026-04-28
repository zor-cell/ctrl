package at.ac.tuwien.inso.ctrl.auth.dto;

public record PasswordResetRequest(String token, String password) {}

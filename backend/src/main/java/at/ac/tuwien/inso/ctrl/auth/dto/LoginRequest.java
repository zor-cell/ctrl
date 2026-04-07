package at.ac.tuwien.inso.ctrl.auth.dto;

public record LoginRequest(
        String username,
        String password
) { }

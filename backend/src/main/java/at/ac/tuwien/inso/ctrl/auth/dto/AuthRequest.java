package at.ac.tuwien.inso.ctrl.auth.dto;

public record AuthRequest(
        String username,
        String password
) { }

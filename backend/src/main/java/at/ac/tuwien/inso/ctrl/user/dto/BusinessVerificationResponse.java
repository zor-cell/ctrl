package at.ac.tuwien.inso.ctrl.user.dto;

import java.util.UUID;

public record BusinessVerificationResponse(UserDetailsResponse user, UUID id) {}

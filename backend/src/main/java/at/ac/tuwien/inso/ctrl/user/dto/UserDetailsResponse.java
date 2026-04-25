package at.ac.tuwien.inso.ctrl.user.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserDetailsResponse(
    UUID id,
    String username,
    String email,
    String name,
    String bio,
    LocalDate birthdate,
    String profilePicture,
    Instant createdAt,
    boolean isVerified) {}

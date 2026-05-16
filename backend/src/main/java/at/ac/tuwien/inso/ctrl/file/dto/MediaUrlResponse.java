package at.ac.tuwien.inso.ctrl.file.dto;

import java.time.Instant;

public record MediaUrlResponse(String mediaUrl, Instant expiresAt) {}

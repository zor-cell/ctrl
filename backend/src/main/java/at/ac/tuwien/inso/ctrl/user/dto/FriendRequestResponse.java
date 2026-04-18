package at.ac.tuwien.inso.ctrl.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FriendRequestResponse(
    UUID id, String username, String profilePicture, boolean isIncoming, LocalDateTime createdAt) {}

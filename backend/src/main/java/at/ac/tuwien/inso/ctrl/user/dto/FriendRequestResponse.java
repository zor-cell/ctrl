package at.ac.tuwien.inso.ctrl.user.dto;

import java.time.LocalDateTime;

/**
 * DTO for friend request responses. The username field always contains the "other" person's
 * username: - For incoming requests: the sender's username - For sent requests: the receiver's
 * username
 */
public record FriendRequestResponse(
    String username, String profilePicture, boolean isIncoming, LocalDateTime createdAt) {}

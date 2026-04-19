package at.ac.tuwien.inso.ctrl.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for PATCH /friend-requests/{username} endpoint.
 * Contains the action to perform on the friend request.
 */
public record FriendRequestActionDto(
    @NotNull(message = "Action is required")
    FriendRequestAction action
) {}

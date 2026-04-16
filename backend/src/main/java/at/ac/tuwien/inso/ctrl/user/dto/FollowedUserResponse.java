package at.ac.tuwien.inso.ctrl.user.dto;

import java.util.UUID;

public record FollowedUserResponse(UUID id, String username, String profilePicture) {}

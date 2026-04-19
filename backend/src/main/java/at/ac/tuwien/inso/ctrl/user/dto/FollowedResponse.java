package at.ac.tuwien.inso.ctrl.user.dto;

import java.util.UUID;

public record FollowedResponse(UUID id, String username, String profilePicture) {}

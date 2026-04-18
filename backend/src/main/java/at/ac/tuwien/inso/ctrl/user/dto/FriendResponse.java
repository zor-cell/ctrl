package at.ac.tuwien.inso.ctrl.user.dto;

import java.util.UUID;

public record FriendResponse(UUID id, String username, String name, String profilePicture) {}

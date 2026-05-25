package at.ac.tuwien.inso.ctrl.chat.dto;

import java.util.UUID;

public record ChatParticipantResponse(
    UUID id, String username, String name, String profilePicture) {}

package at.ac.tuwien.inso.ctrl.user.dto;

import java.util.List;
import java.util.UUID;

public record UserSearchResponse(
    UUID id,
    String username,
    String name,
    String profilePicture,
    boolean isVerified,
    List<RelationshipStatusItem> relationshipStatuses) {}

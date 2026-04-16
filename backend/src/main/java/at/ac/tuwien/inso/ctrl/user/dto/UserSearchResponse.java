package at.ac.tuwien.inso.ctrl.user.dto;

import java.util.UUID;

public record UserSearchResponse(
    UUID id,
    String username,
    String name,
    String profilePicture,
    boolean isVerified,
    RelationshipStatus relationshipStatus) {

  public enum RelationshipStatus {
    NONE,
    FOLLOWING,
    FRIEND,
    PENDING_REQUEST_SENT,
    PENDING_REQUEST_INCOMING
  }
}

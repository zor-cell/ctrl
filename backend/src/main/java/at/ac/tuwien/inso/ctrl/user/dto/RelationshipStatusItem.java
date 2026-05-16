package at.ac.tuwien.inso.ctrl.user.dto;

public record RelationshipStatusItem(RelationshipStatus status) {
  public enum RelationshipStatus {
    FRIEND,
    FOLLOWED,
    PENDING_REQUEST_SENT,
    PENDING_REQUEST_INCOMING
  }
}

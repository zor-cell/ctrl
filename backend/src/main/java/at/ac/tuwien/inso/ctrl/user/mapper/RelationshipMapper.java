package at.ac.tuwien.inso.ctrl.user.mapper;

import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.user.dto.FollowedResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
import at.ac.tuwien.inso.ctrl.user.dto.RelationshipStatusItem;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RelationshipMapper {

  private final FileStorageService fileStorageService;

  public FriendResponse toFriendResponse(User user) {
    if (user == null) {
      return null;
    }

    return new FriendResponse(
        user.getId(),
        user.getUsername(),
        user.getName(),
        fileStorageService.resolveFileUrl(user.getProfilePicture()));
  }

  public FollowedResponse toFollowedResponse(User user) {
    if (user == null) {
      return null;
    }

    return new FollowedResponse(
        user.getId(),
        user.getUsername(),
        fileStorageService.resolveFileUrl(user.getProfilePicture()));
  }

  public FriendRequestResponse toIncomingFriendRequestResponse(FriendRequest request) {
    if (request == null) {
      return null;
    }

    User sender = request.getSender();
    return new FriendRequestResponse(
        sender == null ? null : sender.getUsername(),
        sender == null ? null : fileStorageService.resolveFileUrl(sender.getProfilePicture()),
        true,
        request.getCreatedAt());
  }

  public FriendRequestResponse toSentFriendRequestResponse(FriendRequest request) {
    if (request == null) {
      return null;
    }

    User receiver = request.getReceiver();
    return new FriendRequestResponse(
        receiver == null ? null : receiver.getUsername(),
        receiver == null ? null : fileStorageService.resolveFileUrl(receiver.getProfilePicture()),
        false,
        request.getCreatedAt());
  }

  public UserSearchResponse toUserSearchResponse(
      User user, List<RelationshipStatusItem> relationshipStatuses, String displayName) {
    if (user == null) {
      return null;
    }

    return new UserSearchResponse(
        user.getId(),
        user.getUsername(),
        displayName,
        fileStorageService.resolveFileUrl(user.getProfilePicture()),
        user.isVerified(),
        relationshipStatuses);
  }

  public String displayName(User source, List<RelationshipStatusItem> relationshipStatuses) {
    boolean isFriend =
        relationshipStatuses.stream()
            .anyMatch(item -> item.status() == RelationshipStatusItem.RelationshipStatus.FRIEND);
    if (source.isVerified() || isFriend) {
      return source.getName();
    }
    return null;
  }

  public List<RelationshipStatusItem> determineRelationshipStatuses(
      User targetUser,
      Set<UUID> followedIds,
      Set<UUID> friendIds,
      Set<UUID> pendingRequestSentIds,
      Set<UUID> pendingRequestIncomingIds) {
    List<RelationshipStatusItem> statuses = new ArrayList<>();

    if (friendIds.contains(targetUser.getId())) {
      statuses.add(new RelationshipStatusItem(RelationshipStatusItem.RelationshipStatus.FRIEND));
    }
    if (followedIds.contains(targetUser.getId())) {
      statuses.add(new RelationshipStatusItem(RelationshipStatusItem.RelationshipStatus.FOLLOWED));
    }
    if (pendingRequestSentIds.contains(targetUser.getId())) {
      statuses.add(
          new RelationshipStatusItem(
              RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_SENT));
    }
    if (pendingRequestIncomingIds.contains(targetUser.getId())) {
      statuses.add(
          new RelationshipStatusItem(
              RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_INCOMING));
    }

    return statuses;
  }
}

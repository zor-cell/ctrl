package at.ac.tuwien.inso.ctrl.user.mapper;

import at.ac.tuwien.inso.ctrl.user.dto.FollowedUserResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse.RelationshipStatus;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.Set;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RelationshipMapper {

  // remove when adding profilePicture support
  @Mapping(target = "profilePicture", constant = "null")
  FriendResponse toFriendResponse(User user);

  // remove when adding profilePicture support
  @Mapping(target = "profilePicture", constant = "null")
  FollowedUserResponse toFollowedUserResponse(User user);

  @Mapping(target = "profilePicture", constant = "null")
  @Mapping(target = "username", source = "sender.username")
  @Mapping(target = "isIncoming", constant = "true")
  FriendRequestResponse toIncomingFriendRequestResponse(FriendRequest request);

  @Mapping(target = "profilePicture", constant = "null")
  @Mapping(target = "username", source = "receiver.username")
  @Mapping(target = "isIncoming", constant = "false")
  FriendRequestResponse toSentFriendRequestResponse(FriendRequest request);

  // remove when adding profilePicture support
  @Mapping(target = "profilePicture", constant = "null")
  @Mapping(target = "name", source = "displayName")
  @Mapping(target = "relationshipStatus", source = "relationshipStatus")
  UserSearchResponse toUserSearchResponse(
      User user, UserSearchResponse.RelationshipStatus relationshipStatus, String displayName);

  default String displayName(
      User source, UserSearchResponse.RelationshipStatus relationshipStatus) {
    if (source.isVerified() || relationshipStatus == RelationshipStatus.FRIEND) {
      return source.getName();
    }
    return null;
  }

  default UserSearchResponse.RelationshipStatus determineRelationshipStatus(
      User targetUser,
      Set<UUID> followingIds,
      Set<UUID> friendIds,
      Set<UUID> pendingRequestSentIds,
      Set<UUID> pendingRequestIncomingIds) {
    if (friendIds.contains(targetUser.getId())) {
      return UserSearchResponse.RelationshipStatus.FRIEND;
    } else if (followingIds.contains(targetUser.getId())) {
      return UserSearchResponse.RelationshipStatus.FOLLOWING;
    } else if (pendingRequestSentIds.contains(targetUser.getId())) {
      return UserSearchResponse.RelationshipStatus.PENDING_REQUEST_SENT;
    } else if (pendingRequestIncomingIds.contains(targetUser.getId())) {
      return UserSearchResponse.RelationshipStatus.PENDING_REQUEST_INCOMING;
    } else {
      return UserSearchResponse.RelationshipStatus.NONE;
    }
  }
}

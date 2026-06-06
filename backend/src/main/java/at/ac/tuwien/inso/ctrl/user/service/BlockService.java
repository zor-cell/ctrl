package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.user.dto.BlockedResponse;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelation;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.BlockedRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockService {

  private final FollowService followService;
  private final UserRepository userRepository;
  private final FriendshipService friendshipService;
  private final BlockedRelationRepository blockedRelationRepository;
  private final RelationshipMapper relationshipMapper;

  public void blockUser(User user, String username) {
    User blockedUser = userRepository.findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));

    if (followService.isFollowing(user, blockedUser)) {
      followService.unfollow(user, username);
    }

    if (followService.isFollowing(blockedUser, user)) {
      followService.unfollow(blockedUser, user.getUsername());
    }

    if (followService.areFriends(user, blockedUser)) {
      friendshipService.unfriend(user, username);
    }

    BlockedRelation blockedRelation = BlockedRelation.builder()
        .since(LocalDateTime.now())
        .id(new BlockedRelationId(user.getId(), blockedUser.getId()))
        .blockedUser(blockedUser)
        .user(user).build();

    blockedRelationRepository.save(blockedRelation);


  }

  public void unblockUser(User currentUser, String username) {
    User blockedUser = userRepository.findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));

    BlockedRelationId relationId = new BlockedRelationId(currentUser.getId(), blockedUser.getId());
    if (!blockedRelationRepository.existsById(relationId)) {
      throw new EntityNotFoundException("Blocked relation not found");
    }

    blockedRelationRepository.deleteById(relationId);
  }

  public List<BlockedResponse> getBlockedUsers(User currentUser) {
    List<User> blockedUsers = blockedRelationRepository.findBlockedUserByUser(currentUser);
    return blockedUsers.stream().map(relationshipMapper::toBlockedResponse).toList();
  }
}

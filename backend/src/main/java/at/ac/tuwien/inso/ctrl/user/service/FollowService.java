package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.user.dto.FollowedUserResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowService {

  private final FollowRelationRepository followRelationRepository;
  private final UserRepository userRepository;
  private final RelationshipMapper socialMapper;

  public List<FriendResponse> getFriends(User currentUser, String search) {
    List<User> friends =
        search == null || search.isBlank()
            ? followRelationRepository.findFriends(currentUser)
            : followRelationRepository.findFriendsBySearch(currentUser, search);
    return friends.stream().map(socialMapper::toFriendResponse).toList();
  }

  public List<FollowedUserResponse> getFollowing(User currentUser, String search) {
    List<User> following =
        search == null || search.isBlank()
            ? followRelationRepository.findFollowingByFollower(currentUser)
            : followRelationRepository.findFollowingBySearch(currentUser, search);
    return following.stream().map(socialMapper::toFollowedUserResponse).toList();
  }

  @Transactional
  public void follow(User currentUser, String targetUsername) {
    if (currentUser.getUsername().equalsIgnoreCase(targetUsername)) {
      throw new InvalidRequestException("Cannot follow yourself");
    }

    User target =
        userRepository
            .findByUsername(targetUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User"));

    if (!target.isVerified()) {
      throw new InvalidRequestException("Can only follow verified users");
    }

    if (followRelationRepository.existsByFollowerAndFollowing(currentUser, target)) {
      return; // already following
    }

    FollowRelation relation =
        FollowRelation.builder()
            .id(new FollowRelationId(currentUser.getId(), target.getId()))
            .follower(currentUser)
            .following(target)
            .since(LocalDateTime.now())
            .build();

    followRelationRepository.save(relation);
  }

  @Transactional
  public void unfollow(User currentUser, String targetUsername) {
    User target =
        userRepository
            .findByUsername(targetUsername)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Delete both directions - this breaks friendship if existed
    followRelationRepository.deleteBidirectional(currentUser, target);
  }

  public Set<UUID> getFollowingIds(User currentUser) {
    return followRelationRepository.findFollowingIdsByFollower(currentUser);
  }

  public Set<UUID> getFriendIds(User currentUser) {
    return followRelationRepository.findFriendIds(currentUser);
  }

  public boolean isFollowing(User follower, User following) {
    return followRelationRepository.existsByFollowerAndFollowing(follower, following);
  }

  public boolean areFriends(User user1, User user2) {
    return followRelationRepository.existsByFollowerAndFollowing(user1, user2)
        && followRelationRepository.existsByFollowerAndFollowing(user2, user1);
  }
}

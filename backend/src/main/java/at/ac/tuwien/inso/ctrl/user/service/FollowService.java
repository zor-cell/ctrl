package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.user.dto.FollowedResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
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
  private final FriendRelationRepository friendRelationRepository;
  private final UserRepository userRepository;
  private final RelationshipMapper socialMapper;

  public List<FollowedResponse> getFollowed(User currentUser, String search) {
    List<User> followed =
        search == null || search.isBlank()
            ? followRelationRepository.findFollowedByFollower(currentUser)
            : followRelationRepository.findFollowedBySearch(currentUser, search);
    return followed.stream().map(socialMapper::toFollowedResponse).toList();
  }

  @Transactional
  public void follow(User currentUser, String targetUsername) {
    if (currentUser.getUsername().equals(targetUsername)) {
      throw new InvalidRequestException("Cannot follow yourself");
    }

    User target =
        userRepository
            .findByUsername(targetUsername)
            .orElseThrow(() -> new NotFoundException("User", targetUsername));

    if (target.isSuspended()) {
      throw new NotFoundException("User", targetUsername);
    }

    if (!target.isVerified()) {
      throw new InvalidRequestException("Can only follow verified users");
    }

    if (followRelationRepository.existsByFollowerAndFollowed(currentUser, target)) {
      return; // already following
    }

    FollowRelation relation =
        FollowRelation.builder()
            .id(new FollowRelationId(currentUser.getId(), target.getId()))
            .follower(currentUser)
            .followed(target)
            .since(LocalDateTime.now())
            .build();

    followRelationRepository.save(relation);
  }

  @Transactional
  public void unfollow(User currentUser, String targetUsername) {
    User target =
        userRepository
            .findByUsername(targetUsername)
            .orElseThrow(() -> new NotFoundException("User", targetUsername));

    if (followRelationRepository.deleteByFollowerAndFollowed(currentUser, target) == 0) {
      throw new NotFoundException("Follow", targetUsername);
    }
  }

  public Set<UUID> getFollowedIds(User currentUser) {
    return followRelationRepository.findFollowedIdsByFollower(currentUser);
  }

  public boolean isFollowing(User follower, User followed) {
    return followRelationRepository.existsByFollowerAndFollowed(follower, followed);
  }

  public Set<UUID> getFriendIds(User currentUser) {
    return friendRelationRepository.findFriendIds(currentUser);
  }

  public boolean areFriends(User firstUser, User secondUser) {
    return friendRelationRepository.existsByUsers(firstUser, secondUser);
  }
}

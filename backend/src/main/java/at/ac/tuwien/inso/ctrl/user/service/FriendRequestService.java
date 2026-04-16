package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

  private final FriendRequestRepository friendRequestRepository;
  private final FollowRelationRepository followRelationRepository;
  private final UserRepository userRepository;
  private final RelationshipMapper socialMapper;

  public List<FriendRequestResponse> getIncomingRequests(User currentUser) {
    List<FriendRequest> requests = friendRequestRepository.findAllIncomingRequests(currentUser);
    requests = filterExpiredRequests(requests);
    return requests.stream().map(socialMapper::toIncomingFriendRequestResponse).toList();
  }

  public List<FriendRequestResponse> getSentRequests(User currentUser) {
    List<FriendRequest> requests = friendRequestRepository.findAllSentRequests(currentUser);
    requests = filterExpiredRequests(requests);
    return requests.stream().map(socialMapper::toSentFriendRequestResponse).toList();
  }

  private boolean checkWhetherAndDeleteIfExpired(FriendRequest request) {
    if (request.getExpiresAt().isBefore(LocalDateTime.now())) {
      friendRequestRepository.delete(request);
      return true;
    }
    return false;
  }

  private List<FriendRequest> filterExpiredRequests(List<FriendRequest> requests) {
    List<FriendRequest> validRequests = new ArrayList<>();

    for (FriendRequest request : requests) {
      if (!checkWhetherAndDeleteIfExpired(request)) {
        validRequests.add(request);
      }
    }
    return validRequests;
  }

  @Transactional
  public void sendRequest(User currentUser, String targetUsername) {
    if (currentUser.getUsername().equalsIgnoreCase(targetUsername)) {
      throw new InvalidRequestException("Cannot send friend request to yourself");
    }

    User target =
        userRepository
            .findByUsername(targetUsername)
            .orElseThrow(() -> new NotFoundException("User", targetUsername));

    if (followRelationRepository.existsByFollowerAndFollowing(currentUser, target)
        && followRelationRepository.existsByFollowerAndFollowing(target, currentUser)) {
      throw new InvalidRequestException("You are already friends with this user");
    }

    if (friendRequestRepository.existsBySenderAndReceiver(currentUser, target)) {
      throw new ResourceAlreadyExistsException("Friend request", "receiver", target.getUsername());
    }

    if (friendRequestRepository.existsBySenderAndReceiver(target, currentUser)) {
      throw new InvalidRequestException(
          "This user has already sent you a friend request. Please accept or decline it.");
    }

    // create follow relation immediately if verified, since even if the request is
    // declined one can still follow verified persons
    if (target.isVerified()
        && !followRelationRepository.existsByFollowerAndFollowing(currentUser, target)) {
      FollowRelation follow =
          FollowRelation.builder()
              .id(new FollowRelationId(currentUser.getId(), target.getId()))
              .follower(currentUser)
              .following(target)
              .since(LocalDateTime.now())
              .build();
      followRelationRepository.save(follow);
    }

    // Create friend request
    FriendRequest request =
        FriendRequest.builder()
            .sender(currentUser)
            .receiver(target)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(30)) // expires in 30 days
            .build();

    friendRequestRepository.save(request);
  }

  @Transactional
  public void acceptRequest(User currentUser, UUID requestId) {
    FriendRequest request =
        friendRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new NotFoundException("Friend request", requestId.toString()));

    // Verify this request is for the current user
    if (!request.getReceiver().getId().equals(currentUser.getId())) {
      throw new InvalidRequestException(
          "You cannot accept this friend request, since you are not the receiver");
    }

    if (checkWhetherAndDeleteIfExpired(request)) {
      throw new InvalidRequestException("This friend request has expired");
    }

    User sender = request.getSender();

    // Create follow relation from receiver to sender (if not exists)
    // This creates the mutual follow = friendship
    if (!followRelationRepository.existsByFollowerAndFollowing(currentUser, sender)) {
      FollowRelation follow =
          FollowRelation.builder()
              .id(new FollowRelationId(currentUser.getId(), sender.getId()))
              .follower(currentUser)
              .following(sender)
              .since(LocalDateTime.now())
              .build();
      followRelationRepository.save(follow);
    }

    // If sender is not verified, also create the reverse follow
    // (normally this would have been created when request was sent, but only if
    // target was verified)
    if (!followRelationRepository.existsByFollowerAndFollowing(sender, currentUser)) {
      FollowRelation reverseFollow =
          FollowRelation.builder()
              .id(new FollowRelationId(sender.getId(), currentUser.getId()))
              .follower(sender)
              .following(currentUser)
              .since(LocalDateTime.now())
              .build();
      followRelationRepository.save(reverseFollow);
    }

    // Delete the request
    friendRequestRepository.delete(request);
  }

  @Transactional
  public void declineRequest(User currentUser, UUID requestId) {
    FriendRequest request =
        friendRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new NotFoundException("Friend request", requestId.toString()));

    // Verify this request is for the current user (incoming) or from current user
    // (sent)
    boolean isReceiver = request.getReceiver().getId().equals(currentUser.getId());
    boolean isSender = request.getSender().getId().equals(currentUser.getId());

    if (!isReceiver && !isSender) {
      throw new InvalidRequestException(
          "You cannot decline this friend request, since you are neither sender nor receiver");
    }

    if (checkWhetherAndDeleteIfExpired(request)) {
      throw new InvalidRequestException("This friend request has expired");
    }

    User sender = request.getSender();
    User receiver = request.getReceiver();

    // If the target (receiver) declines, and they are not verified,
    // remove the follow relation that was created when request was sent
    // (since it was only created if receiver was verified)
    if (isReceiver && !receiver.isVerified()) {
      // Only remove if it exists (would have been created when request sent if
      // receiver verified)
      followRelationRepository.deleteByFollowerAndFollowing(sender, receiver);
    }

    // Delete the request
    friendRequestRepository.delete(request);
  }

  @Transactional
  public long getPendingRequestCount(User currentUser) {
    return friendRequestRepository.findAllIncomingRequests(currentUser).stream()
        .filter(this::checkWhetherAndDeleteIfExpired)
        .count();
  }
}

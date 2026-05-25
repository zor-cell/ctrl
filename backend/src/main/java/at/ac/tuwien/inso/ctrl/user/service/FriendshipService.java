package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestAction;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendshipService {

  private final FriendRequestRepository friendRequestRepository;
  private final FriendRelationRepository friendRelationRepository;
  private final FollowRelationRepository followRelationRepository;
  private final UserRepository userRepository;
  private final RelationshipMapper socialMapper;

  public List<FriendResponse> getFriends(User currentUser, String search) {
    List<User> friends =
        search == null || search.isBlank()
            ? friendRelationRepository.findFriends(currentUser)
            : friendRelationRepository.findFriendsBySearch(currentUser, search.trim());

    return friends.stream().map(socialMapper::toFriendResponse).toList();
  }

  @Transactional
  public void unfriend(User currentUser, String usernameToUnfriend) {
    User toUnfriend =
        userRepository
            .findByUsername(usernameToUnfriend)
            .orElseThrow(() -> new NotFoundException("User", usernameToUnfriend));

    friendRelationRepository.deleteFriendship(currentUser, toUnfriend);
  }

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
    if (currentUser.getUsername().equals(targetUsername)) {
      throw new InvalidRequestException("Cannot send friend request to yourself");
    }

    User target =
        userRepository
            .findByUsername(targetUsername)
            .orElseThrow(() -> new NotFoundException("User", targetUsername));

    if (target.isSuspended()) {
      throw new NotFoundException("User", targetUsername);
    }

    if (friendRelationRepository.existsByUsers(currentUser, target)) {
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
    if (target.isVerified()) {
      FollowRelation follow =
          FollowRelation.builder()
              .id(new FollowRelationId(currentUser.getId(), target.getId()))
              .follower(currentUser)
              .followed(target)
              .since(LocalDateTime.now())
              .build();
      followRelationRepository.save(follow);
    }

    FriendRequest request =
        FriendRequest.builder()
            .id(new FriendRequestId(currentUser.getId(), target.getId()))
            .sender(currentUser)
            .receiver(target)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(30)) // expires in 30 days
            .build();

    friendRequestRepository.save(request);
  }

  @Transactional
  public void handleIncomingRequestAction(
      User currentUser, String senderUsername, FriendRequestAction action) {
    User sender =
        userRepository
            .findByUsername(senderUsername)
            .orElseThrow(() -> new NotFoundException("User", senderUsername));

    if (sender.isSuspended()) {
      throw new NotFoundException("User", senderUsername);
    }

    FriendRequest request =
        friendRequestRepository
            .findBySenderAndReceiver(sender, currentUser)
            .orElseThrow(() -> new NotFoundException("Friend request", "from " + senderUsername));

    // Verify this request is for the current user (receiver)
    if (!request.getReceiver().getId().equals(currentUser.getId())) {
      throw new InvalidRequestException(
          "You cannot manage this friend request, since you are not the receiver");
    }

    if (checkWhetherAndDeleteIfExpired(request)) {
      throw new InvalidRequestException("This friend request has expired");
    }

    switch (action) {
      case ACCEPT:
        acceptRequestInternal(currentUser, sender, request);
        break;
      case DECLINE:
        declineRequestInternal(currentUser, sender, request);
        break;
    }
  }

  private void acceptRequestInternal(User currentUser, User sender, FriendRequest request) {

    followRelationRepository.deleteByFollowerAndFollowed(currentUser, sender);
    followRelationRepository.deleteByFollowerAndFollowed(sender, currentUser);

    FriendRelation friendRelation =
        FriendRelation.builder()
            .id(new FriendRelationId(currentUser.getId(), sender.getId()))
            .friend1(currentUser)
            .friend2(sender)
            .since(LocalDateTime.now())
            .build();

    friendRelationRepository.save(friendRelation);

    friendRequestRepository.delete(request);
  }

  private void declineRequestInternal(User currentUser, User sender, FriendRequest request) {
    friendRequestRepository.delete(request);
  }

  @Transactional
  public void cancelSentRequest(User currentUser, String receiverUsername) {
    User receiver =
        userRepository
            .findByUsername(receiverUsername)
            .orElseThrow(() -> new NotFoundException("User", receiverUsername));

    FriendRequest request =
        friendRequestRepository
            .findBySenderAndReceiver(currentUser, receiver)
            .orElseThrow(() -> new NotFoundException("Friend request", "to " + receiverUsername));

    // Verify this request is from the current user (sender)
    if (!request.getSender().getId().equals(currentUser.getId())) {
      throw new InvalidRequestException(
          "You cannot cancel this friend request, since you are not the sender");
    }

    if (checkWhetherAndDeleteIfExpired(request)) {
      throw new InvalidRequestException("This friend request has expired");
    }

    friendRequestRepository.delete(request);
  }

  @Transactional
  public long getPendingRequestCount(User currentUser) {
    List<FriendRequest> incomingRequests =
        friendRequestRepository.findAllIncomingRequests(currentUser);
    List<FriendRequest> validRequests = filterExpiredRequests(incomingRequests);
    return validRequests.size();
  }
}

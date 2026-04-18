package at.ac.tuwien.inso.ctrl.user;

import at.ac.tuwien.inso.ctrl.user.dto.*;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import at.ac.tuwien.inso.ctrl.user.service.FriendRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class RelationshipController {

  private final FollowService followService;
  private final FriendRequestService friendRequestService;

  @GetMapping("/friends")
  public List<FriendResponse> getFriends(
      @AuthenticationPrincipal User currentUser, @RequestParam(required = false) String q) {
    return followService.getFriends(currentUser, q);
  }

  @GetMapping("/following")
  public List<FollowedUserResponse> getFollowing(
      @AuthenticationPrincipal User currentUser, @RequestParam(required = false) String q) {
    return followService.getFollowing(currentUser, q);
  }

  @PostMapping("/following/{username}")
  @ResponseStatus(HttpStatus.CREATED)
  public void follow(@AuthenticationPrincipal User currentUser, @PathVariable String username) {
    followService.follow(currentUser, username);
  }

  @DeleteMapping("/following/{username}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unfollow(@AuthenticationPrincipal User currentUser, @PathVariable String username) {
    followService.unfollow(currentUser, username);
  }

  @GetMapping("/friend-requests/sent")
  public List<FriendRequestResponse> getSentFriendRequests(
      @AuthenticationPrincipal User currentUser) {
    return friendRequestService.getSentRequests(currentUser);
  }

  @GetMapping("/friend-requests/incoming")
  public List<FriendRequestResponse> getIncomingFriendRequests(
      @AuthenticationPrincipal User currentUser) {
    return friendRequestService.getIncomingRequests(currentUser);
  }

  @PostMapping("/friend-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public void sendFriendRequest(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody SendFriendRequest request) {
    friendRequestService.sendRequest(currentUser, request.toUsername());
  }

  @PostMapping("/friend-requests/{id}/accept")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void acceptFriendRequest(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
    friendRequestService.acceptRequest(currentUser, id);
  }

  @DeleteMapping("/friend-requests/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void declineFriendRequest(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
    friendRequestService.declineRequest(currentUser, id);
  }

  @GetMapping("/friend-requests/count")
  public ResponseEntity<Long> getPendingRequestCount(@AuthenticationPrincipal User currentUser) {
    return ResponseEntity.ok(friendRequestService.getPendingRequestCount(currentUser));
  }
}

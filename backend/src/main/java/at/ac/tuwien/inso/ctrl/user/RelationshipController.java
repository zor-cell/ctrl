package at.ac.tuwien.inso.ctrl.user;

import at.ac.tuwien.inso.ctrl.user.dto.*;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import at.ac.tuwien.inso.ctrl.user.service.FriendshipService;
import jakarta.validation.Valid;
import java.util.List;
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
  private final FriendshipService friendshipService;

  @GetMapping("/friends")
  public List<FriendResponse> getFriends(
      @AuthenticationPrincipal User currentUser, @RequestParam(required = false) String q) {
    return friendshipService.getFriends(currentUser, q);
  }

  @DeleteMapping("/friends/{username}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unfriend(@AuthenticationPrincipal User currentUser, @PathVariable String username) {
    friendshipService.unfriend(currentUser, username);
  }

  @GetMapping("/followed")
  public List<FollowedResponse> getFollowed(
      @AuthenticationPrincipal User currentUser, @RequestParam(required = false) String q) {
    return followService.getFollowed(currentUser, q);
  }

  @PostMapping("/followed/{username}")
  @ResponseStatus(HttpStatus.CREATED)
  public void follow(@AuthenticationPrincipal User currentUser, @PathVariable String username) {
    followService.follow(currentUser, username);
  }

  @DeleteMapping("/followed/{username}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unfollow(@AuthenticationPrincipal User currentUser, @PathVariable String username) {
    followService.unfollow(currentUser, username);
  }

  @GetMapping("/friend-requests/sent")
  public List<FriendRequestResponse> getSentFriendRequests(
      @AuthenticationPrincipal User currentUser) {
    return friendshipService.getSentRequests(currentUser);
  }

  @GetMapping("/friend-requests/incoming")
  public List<FriendRequestResponse> getIncomingFriendRequests(
      @AuthenticationPrincipal User currentUser) {
    return friendshipService.getIncomingRequests(currentUser);
  }

  @PostMapping("/friend-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public void sendFriendRequest(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody SendFriendRequest request) {
    friendshipService.sendRequest(currentUser, request.toUsername());
  }

  @PatchMapping("/friend-requests/{username}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void handleFriendRequestAction(
      @AuthenticationPrincipal User currentUser,
      @PathVariable String username,
      @Valid @RequestBody FriendRequestActionDto actionDto) {
    friendshipService.handleIncomingRequestAction(currentUser, username, actionDto.action());
  }

  @DeleteMapping("/friend-requests/{username}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelFriendRequest(
      @AuthenticationPrincipal User currentUser, @PathVariable String username) {
    friendshipService.cancelSentRequest(currentUser, username);
  }

  @GetMapping("/friend-requests/count")
  public ResponseEntity<Long> getPendingRequestCount(@AuthenticationPrincipal User currentUser) {
    return ResponseEntity.ok(friendshipService.getPendingRequestCount(currentUser));
  }
}

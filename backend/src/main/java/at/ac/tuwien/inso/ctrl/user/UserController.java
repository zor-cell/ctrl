package at.ac.tuwien.inso.ctrl.user;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.user.dto.*;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public UserDetailsResponse getUser(@AuthenticationPrincipal User currentUser) {
    return userService.getUser(currentUser.getId());
  }

  @PutMapping("/me")
  public UserDetailsResponse updateUser(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateUser(currentUser, request);
  }

  @PutMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updatePassword(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody UpdatePasswordRequest request) {
    userService.updatePassword(currentUser, request);
  }

  @PutMapping("/me/username")
  public AuthResponse updateUsername(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody UpdateUsernameRequest request) {
    return userService.updateUsername(currentUser, request);
  }

  @PutMapping("/me/verify")
  public UserDetailsResponse verifyUser(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody VerifyIdentityRequest request) {
    return userService.verifyUser(currentUser, request);
  }

  @GetMapping("/search")
  public List<UserSearchResponse> searchUsers(
      @AuthenticationPrincipal User currentUser,
      @RequestParam String q,
      @RequestParam(required = false, defaultValue = "false") boolean excludeFriends,
      @RequestParam(required = false, defaultValue = "false") boolean excludeFollowing,
      @RequestParam(required = false, defaultValue = "false") boolean verifiedOnly) {

    return userService.searchUsers(currentUser, q, excludeFriends, excludeFollowing, verifiedOnly);
  }
}

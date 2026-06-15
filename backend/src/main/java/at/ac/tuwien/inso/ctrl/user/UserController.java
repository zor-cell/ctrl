package at.ac.tuwien.inso.ctrl.user;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.user.dto.PublicUserProfileResponse;
import at.ac.tuwien.inso.ctrl.user.dto.UpdatePasswordRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateProfileRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateUsernameRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.dto.VerificationStatusResponse;
import at.ac.tuwien.inso.ctrl.user.dto.VerifyIdentityRequest;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.service.BusinessVerificationService;
import at.ac.tuwien.inso.ctrl.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final BusinessVerificationService businessVerificationService;

  @GetMapping("/me")
  public UserDetailsResponse getUser(@AuthenticationPrincipal User currentUser) {
    return userService.getUser(currentUser.getId());
  }

  @GetMapping("/{username}")
  public PublicUserProfileResponse getPublicProfile(
      @PathVariable String username, @AuthenticationPrincipal User currentUser) {
    return userService.getPublicProfile(username, currentUser);
  }

  @PutMapping("/me")
  public UserDetailsResponse updateUser(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody UpdateProfileRequest request) {
    return userService.updateUser(currentUser, request);
  }

  @PutMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UserDetailsResponse updateProfilePicture(
      @AuthenticationPrincipal User currentUser, @RequestPart("file") MultipartFile file) {
    return userService.updateProfilePicture(currentUser, file);
  }

  @DeleteMapping("/me/profile-picture")
  public UserDetailsResponse deleteProfilePicture(@AuthenticationPrincipal User currentUser) {
    return userService.deleteProfilePicture(currentUser);
  }

  @DeleteMapping("/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCurrentUser(@AuthenticationPrincipal User currentUser) {
    userService.deleteCurrentUser(currentUser);
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

  @PostMapping("/me/verify/business")
  public void submitBusinessVerification(@AuthenticationPrincipal User currentUser) {
    businessVerificationService.submitBusinessVerification(currentUser);
  }

  @GetMapping("/me/verify")
  public VerificationStatusResponse getVerificationStatus(
      @AuthenticationPrincipal User currentUser) {
    return businessVerificationService.getVerificationStatus(currentUser);
  }
}

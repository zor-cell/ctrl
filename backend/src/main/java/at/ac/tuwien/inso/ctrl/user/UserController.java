package at.ac.tuwien.inso.ctrl.user;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.user.dto.UpdatePasswordRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateProfileRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateUsernameRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.service.UserService;
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
        return userService.getProfile(currentUser.getId());
    }

    @PutMapping("/me")
    public UserDetailsResponse updateUser(@AuthenticationPrincipal User currentUser, @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser, request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(@AuthenticationPrincipal User currentUser, @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(currentUser, request);
    }

    @PutMapping("/me/username")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public AuthResponse updateUsername(@AuthenticationPrincipal User currentUser, @RequestBody UpdateUsernameRequest request) {
        return userService.updateUsername(currentUser, request);
    }
}

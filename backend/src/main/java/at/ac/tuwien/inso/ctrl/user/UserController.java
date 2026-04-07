package at.ac.tuwien.inso.ctrl.user;

import at.ac.tuwien.inso.ctrl.user.dto.UpdateProfileRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserDetailsResponse getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return userService.getProfile(currentUser.getId());
    }

    @PutMapping("/me")
    public UserDetailsResponse getCurrentUser(@AuthenticationPrincipal User currentUser, @RequestBody UpdateProfileRequest updateProfileRequest) {
        return userService.updateProfile(currentUser, updateProfileRequest);
    }
}

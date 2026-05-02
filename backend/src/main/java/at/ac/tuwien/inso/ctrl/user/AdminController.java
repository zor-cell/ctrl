package at.ac.tuwien.inso.ctrl.user;

import at.ac.tuwien.inso.ctrl.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;

    @GetMapping("/verifications/pending")
    public void getPendingRequests() {

    }

    @PostMapping("/verifications/{requestId}/decision")
    public void reviewRequest(
            @PathVariable UUID requestId,
            @RequestBody Object decision) {

    }
}

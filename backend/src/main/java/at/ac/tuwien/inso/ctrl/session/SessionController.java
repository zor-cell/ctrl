package at.ac.tuwien.inso.ctrl.session;

import at.ac.tuwien.inso.ctrl.session.dto.SessionConfigDto;
import at.ac.tuwien.inso.ctrl.session.services.SessionService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {
  private final SessionService sessionService;

  @PostMapping("/heartbeat")
  public void handleHeartbeat(@AuthenticationPrincipal User user) {
    // timestamp is generated here to better test the service, shouldn't make a
    // difference anyways
    sessionService.handleHeartbeat(LocalDateTime.now(), user);
  }

  @GetMapping("/config")
  public SessionConfigDto getConfig(@AuthenticationPrincipal User user) {
    return sessionService.getConfig(user);
  }

  @PutMapping("/config")
  public SessionConfigDto updateConfig(
      @AuthenticationPrincipal User user, @Valid @RequestBody SessionConfigDto config) {
    return sessionService.updateConfig(user, config);
  }

  @PostMapping("/soft-cutoff")
  public void resetSoftCutoff(@AuthenticationPrincipal User user) {
    sessionService.resetSoftCutoff(user);
  }
}

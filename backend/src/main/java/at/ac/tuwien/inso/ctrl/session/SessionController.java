package at.ac.tuwien.inso.ctrl.session;

import at.ac.tuwien.inso.ctrl.session.services.SessionService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SessionController {
  private final SessionService sessionService;

  @PostMapping("/sessions/heartbeat")
  public void handleHeatbeat(@AuthenticationPrincipal User user) {
    // timestamp is generated here to better test the service, shouldn't make a
    // difference anyways
    sessionService.handleHeatbeat(LocalDateTime.now(), user);
  }
}

package at.ac.tuwien.inso.ctrl.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AngularForwardController {

  // forwards everything which does not start with api, public or swagger and
  // which does not contain any dots (static images like icons) to index.html so
  // angular routing can kick in
  @GetMapping({
    "{path:^(?!api|ws|public|swagger)[^\\.]*}/{subPath:[^\\.]*}",
    "{path:^(?!api|ws|public|swagger)[^\\.]*}"
  })
  public String handleForward() {
    return "forward:/index.html";
  }
}

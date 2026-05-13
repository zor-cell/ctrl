package at.ac.tuwien.inso.ctrl.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AngularForwardController {

  @GetMapping({
    "{path:^(?!api|public|swagger)[^\\.]*}/{subPath:[^\\.]*}",
    "{path:^(?!api|public|swagger)[^\\.]*}"
  })
  public String handleForward() {
    return "forward:/index.html";
  }
}

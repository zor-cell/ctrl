package at.ac.tuwien.inso.ctrl.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootForwardController {

  @GetMapping("/")
  public String forwardRoot() {
    return "forward:/index.html";
  }
}

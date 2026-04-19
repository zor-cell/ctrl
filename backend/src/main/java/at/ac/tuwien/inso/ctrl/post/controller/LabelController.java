package at.ac.tuwien.inso.ctrl.post.controller;

import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import at.ac.tuwien.inso.ctrl.post.service.LabelService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/labels")
@RequiredArgsConstructor
public class LabelController {
  private final LabelService labelService;

  @GetMapping
  public List<LabelResponse> getAvailableLabels() {
    return labelService.getAvailableLabels();
  }
}

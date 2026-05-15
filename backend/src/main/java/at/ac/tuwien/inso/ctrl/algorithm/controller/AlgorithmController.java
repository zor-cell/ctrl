package at.ac.tuwien.inso.ctrl.algorithm.controller;

import at.ac.tuwien.inso.ctrl.algorithm.dto.AlgorithmConfigDto;
import at.ac.tuwien.inso.ctrl.algorithm.service.AlgorithmService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/algorithm")
@RequiredArgsConstructor
public class AlgorithmController {
  private final AlgorithmService algorithmService;

  @GetMapping("/config")
  public AlgorithmConfigDto getConfig(@AuthenticationPrincipal User user) {
    return algorithmService.getConfig(user);
  }

  @PutMapping("/config")
  public AlgorithmConfigDto updateConfig(
      @AuthenticationPrincipal User user, @Valid @RequestBody AlgorithmConfigDto config) {
    return algorithmService.updateConfig(user, config);
  }
}

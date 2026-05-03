package at.ac.tuwien.inso.ctrl.algorithm.controller;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.AlgorithmConfigResponse;
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
    public AlgorithmConfigResponse getConfig(
            @AuthenticationPrincipal User user) {
        return algorithmService.getConfig(user);
    }

    @PutMapping("/config")
    public AlgorithmConfigResponse updateConfig(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AlgorithmConfigResponse config) {
        return algorithmService.updateConfig(user, config);
    }
}

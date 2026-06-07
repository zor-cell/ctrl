package at.ac.tuwien.inso.ctrl.session.controller;

import at.ac.tuwien.inso.ctrl.session.dto.*;
import at.ac.tuwien.inso.ctrl.session.services.AnalyticsService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  @GetMapping("/last-week")
  public List<LastWeekDayResponse> getLastWeekSessions(@AuthenticationPrincipal User user) {
    return analyticsService.getLastWeek(user);
  }

  @GetMapping("/last-week/detailed")
  public List<LastWeekDayDetailResponse> getDaySessionDetail(@AuthenticationPrincipal User user) {
    return analyticsService.getLastWeekDetails(user);
  }

  @GetMapping("/weekly-average")
  public List<AverageDayResponse> getWeeklyAverage(@AuthenticationPrincipal User user) {
    return analyticsService.getAverageWeek(user);
  }

  @GetMapping("/weekly-average/detailed")
  public List<AverageDayDetailResponse> getWeeklyAverageDetails(
      @AuthenticationPrincipal User user) {
    return analyticsService.getAverageWeekDetails(user);
  }
}

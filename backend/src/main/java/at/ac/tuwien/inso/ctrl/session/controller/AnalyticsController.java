package at.ac.tuwien.inso.ctrl.session.controller;

import at.ac.tuwien.inso.ctrl.session.dto.*;
import at.ac.tuwien.inso.ctrl.session.services.AnalyticsService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  @GetMapping("/last-week")
  public LastWeekSessionsResponse getLastWeekSessions(@AuthenticationPrincipal User user) {
    return analyticsService.getLastWeekSessions(user);
  }

  @GetMapping("/last-week/{date}")
  public DaySessionDetailResponse getDaySessionDetail(
      @AuthenticationPrincipal User user,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return analyticsService.getDaySessionDetail(user, date);
  }

  @GetMapping("/weekly-average")
  public WeeklyAverageResponse getWeeklyAverage(@AuthenticationPrincipal User user) {
    return analyticsService.getSixtyDayAverages(user);
  }

  @GetMapping("/weekly-average/{dayOfWeek}")
  public DayOfWeekDetailResponse getDayOfWeekDetail(
      @AuthenticationPrincipal User user, @PathVariable int dayOfWeek) {
    return analyticsService.getDayOfWeekDetail(user, dayOfWeek);
  }
}

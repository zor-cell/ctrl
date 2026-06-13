package at.ac.tuwien.inso.ctrl.session.services;

import at.ac.tuwien.inso.ctrl.session.dto.*;
import at.ac.tuwien.inso.ctrl.session.entity.DailySummary;
import at.ac.tuwien.inso.ctrl.session.entity.DayOfWeekAverage;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.mapper.AnalyticsMapper;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

  private final Clock clock;
  private final SessionRepository sessionRepository;
  private final AnalyticsMapper analyticsMapper;

  @Value("${analytics.avg-timeframe-weeks:8}")
  private int avgTimeframeInWeeks;

  @Transactional
  public List<LastWeekDayResponse> getWeek(User user, int weeksAgo) {
    LocalDate today = LocalDate.now(clock);

    LocalDate targetWeek = today.minusWeeks(weeksAgo);

    LocalDate weekStart = targetWeek.with(DayOfWeek.MONDAY);

    LocalDate weekEnd = weekStart.plusDays(7);

    List<DailySummary> days = sessionRepository.findSummaries(user.getId(), weekStart, weekEnd);
    return analyticsMapper.toLastWeekDayList(days);
  }

  @Transactional
  public List<LastWeekDayDetailResponse> getWeekDetails(User user, int weeksAgo) {
    LocalDateTime today = LocalDateTime.now(clock);

    LocalDateTime targetWeek = today.minusWeeks(weeksAgo);

    LocalDateTime weekStart = targetWeek.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();

    LocalDateTime weekEnd =
        weekStart.plusDays(6).withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);

    List<SessionHistory> sessions =
        sessionRepository.findSessionsForPeriod(user.getId(), weekStart, weekEnd);
    return analyticsMapper.toLastWeekDayDetailResponseList(sessions);
  }

  @Transactional
  public List<AverageDayResponse> getAverageWeek(User user) {
    LocalDate today = LocalDate.now(clock);
    long timeframeInDays =
        Math.min(
            ChronoUnit.DAYS.between(
                user.getCreatedAt().atZone(clock.getZone()).toLocalDate(), today),
            avgTimeframeInWeeks * 7);
    List<DayOfWeekAverage> days =
        sessionRepository.findDayOfWeekAverages(
            user.getId(), today.minusDays(timeframeInDays), today);
    return analyticsMapper.toAverageDayResponseList(days);
  }

  @Transactional
  public List<AverageDayDetailResponse> getAverageWeekDetails(User user) {
    LocalDate today = LocalDate.now(clock);
    long timeframeInDays =
        Math.min(
            ChronoUnit.DAYS.between(
                user.getCreatedAt().atZone(clock.getZone()).toLocalDate(), today),
            avgTimeframeInWeeks * 7);
    List<SessionHistory> sessions =
        sessionRepository.findSessionsForPeriod(
            user.getId(), today.minusDays(timeframeInDays).atStartOfDay(), today.atStartOfDay());

    return analyticsMapper.toAverageDayDetailResponseList(sessions, timeframeInDays);
  }
}

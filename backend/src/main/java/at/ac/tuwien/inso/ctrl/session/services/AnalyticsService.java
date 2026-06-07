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
import java.time.Instant;
import java.time.LocalDate;
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
  public List<LastWeekDayResponse> getLastWeek(User user) {
    List<DailySummary> days = sessionRepository.findLastWeekSummary(user.getId());
    return analyticsMapper.toLastWeekDayList(days);
  }

  @Transactional
  public List<LastWeekDayDetailResponse> getLastWeekDetails(User user) {
    List<SessionHistory> sessions =
        sessionRepository.findSessionsForLastWeek(
            user,
            LocalDate.now(clock).minusDays(7).atStartOfDay(),
            LocalDate.now(clock).atStartOfDay());
    return analyticsMapper.toLastWeekDayDetailResponseList(sessions);
  }

  @Transactional
  public List<AverageDayResponse> getAverageWeek(User user) {
    long timeframeInDays =
        Math.min(
            ChronoUnit.DAYS.between(user.getCreatedAt(), Instant.now(clock)),
            avgTimeframeInWeeks * 7);
    List<DayOfWeekAverage> days =
        sessionRepository.findDayOfWeekAverages(user.getId(), (int) timeframeInDays);
    return analyticsMapper.toAverageDayResponseList(days);
  }

  @Transactional
  public List<AverageDayDetailResponse> getAverageWeekDetails(User user) {
    long timeframeInDays =
        Math.min(
            ChronoUnit.DAYS.between(user.getCreatedAt(), Instant.now(clock)),
            avgTimeframeInWeeks * 7);
    List<SessionHistory> sessions =
        sessionRepository.findSessionsForPeriod(user.getId(), timeframeInDays);

    return analyticsMapper.toAverageDayDetailResponseList(sessions, timeframeInDays);
  }
}

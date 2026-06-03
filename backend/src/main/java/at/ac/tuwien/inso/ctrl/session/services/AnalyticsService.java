package at.ac.tuwien.inso.ctrl.session.services;

import at.ac.tuwien.inso.ctrl.session.dto.*;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.mapper.AnalyticsMapper;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

  private final SessionRepository sessionRepository;
  private final AnalyticsMapper analyticsMapper;

  @Transactional
  public LastWeekSessionsResponse getLastWeekSessions(User user) {
    List<Object[]> rows = sessionRepository.findLastSevenDaysSummary(user.getId());
    return new LastWeekSessionsResponse(analyticsMapper.toWeeklySessionDayDtos(rows));
  }

  @Transactional
  public DaySessionDetailResponse getDaySessionDetail(User user, LocalDate date) {
    LocalDateTime dayStart = date.atStartOfDay();
    LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
    List<SessionHistory> sessions = sessionRepository.findSessionsForDay(user, dayStart, dayEnd);
    List<SessionIntervalDto> intervals =
        sessions.stream().map(s -> new SessionIntervalDto(s.getStart(), s.getMinutes())).toList();
    return new DaySessionDetailResponse(date, intervals);
  }

  @Transactional
  public WeeklyAverageResponse getSixtyDayAverages(User user) {
    List<Object[]> rows = sessionRepository.findSixtyDayDayOfWeekAverages(user.getId());
    return new WeeklyAverageResponse(analyticsMapper.toDayOfWeekAverageDtos(rows));
  }

  @Transactional
  public DayOfWeekDetailResponse getDayOfWeekDetail(User user, int dayOfWeek) {
    if (dayOfWeek < 1 || dayOfWeek > 7) {
      throw new IllegalArgumentException("dayOfWeek must be between 1 (Monday) and 7 (Sunday)");
    }

    List<SessionHistory> sessions =
        sessionRepository.findSessionsForDayOfWeekInPeriod(user.getId(), dayOfWeek);
    long occurrences = countOccurrences(dayOfWeek, LocalDate.now().minusDays(60), LocalDate.now());

    double[] totalMinutes = new double[96];
    @SuppressWarnings("unchecked")
    Set<UUID>[] contributors = new Set[96];
    for (int i = 0; i < 96; i++) {
      contributors[i] = new HashSet<>();
    }

    for (SessionHistory session : sessions) {
      LocalDateTime start = session.getStart();
      long duration = session.getMinutes();
      LocalDateTime end = start.plusMinutes(duration);

      int startMin = start.getHour() * 60 + start.getMinute();
      int endMin = end.getHour() * 60 + end.getMinute();

      if (end.toLocalDate().isAfter(start.toLocalDate())) {
        endMin = 24 * 60;
      }

      for (int bucket = 0; bucket < 96; bucket++) {
        int bucketStart = bucket * 15;
        int bucketEnd = (bucket + 1) * 15;
        int overlap = Math.max(0, Math.min(endMin, bucketEnd) - Math.max(startMin, bucketStart));
        if (overlap > 0) {
          totalMinutes[bucket] += overlap;
          contributors[bucket].add(session.getSessionId());
        }
      }
    }

    List<TimeBucketDto> buckets = new ArrayList<>(96);
    for (int i = 0; i < 96; i++) {
      int h = i / 4;
      int m = (i % 4) * 15;
      int h2 = m == 45 ? h + 1 : h;
      int m2 = m == 45 ? 0 : m + 15;
      String label = String.format("%02d:%02d-%02d:%02d", h, m, h2, m2);
      double avg = occurrences > 0 ? totalMinutes[i] / occurrences : 0.0;
      buckets.add(new TimeBucketDto(i, label, avg, contributors[i].size()));
    }

    return new DayOfWeekDetailResponse(dayOfWeek, buckets);
  }

  private long countOccurrences(int dayOfWeek, LocalDate fromInclusive, LocalDate toExclusive) {
    long count = 0;
    LocalDate cursor = fromInclusive;
    while (cursor.isBefore(toExclusive)) {
      if (cursor.getDayOfWeek().getValue() == dayOfWeek) {
        count++;
      }
      cursor = cursor.plusDays(1);
    }
    return count;
  }
}

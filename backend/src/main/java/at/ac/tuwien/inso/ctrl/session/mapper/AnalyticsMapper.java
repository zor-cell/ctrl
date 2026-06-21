package at.ac.tuwien.inso.ctrl.session.mapper;

import at.ac.tuwien.inso.ctrl.session.dto.AverageDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.AverageDayResponse;
import at.ac.tuwien.inso.ctrl.session.dto.AverageTimeBucketResponse;
import at.ac.tuwien.inso.ctrl.session.dto.LastWeekDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.LastWeekDayResponse;
import at.ac.tuwien.inso.ctrl.session.dto.SessionIntervalDto;
import at.ac.tuwien.inso.ctrl.session.dto.TimeBucket;
import at.ac.tuwien.inso.ctrl.session.entity.DailySummary;
import at.ac.tuwien.inso.ctrl.session.entity.DayOfWeekAverage;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AnalyticsMapper {
  private final int TIME_BUCKETS_PER_DAY = 96;
  private final Clock clock;

  public LastWeekDayResponse toLastWeekDayDto(DailySummary day) {
    return new LastWeekDayResponse(
        day.getDay(), Math.round(day.getTotalSeconds() / 60.0), day.getSessionCount());
  }

  public List<LastWeekDayResponse> toLastWeekDayList(List<DailySummary> days) {
    return days.stream().map(this::toLastWeekDayDto).toList();
  }

  public AverageDayResponse toAverageDayResponse(DayOfWeekAverage day) {
    return new AverageDayResponse(
        day.getDayOfWeek() - 1,
        Math.round(day.getAvgSeconds() / 60.0),
        day.getAvgSessionCount(),
        day.getDaysInPeriod());
  }

  public List<AverageDayResponse> toAverageDayResponseList(List<DayOfWeekAverage> days) {
    return days.stream().map(this::toAverageDayResponse).toList();
  }

  public List<LastWeekDayDetailResponse> toLastWeekDayDetailResponseList(
      List<SessionHistory> sessions) {
    Map<LocalDate, List<SessionIntervalDto>> sessionMap = this.toSessionIntervalDtoMap(sessions);

    LocalDate earliest = sessionMap.keySet().stream().min(LocalDate::compareTo).orElseThrow();
    LocalDate monday = earliest.with(DayOfWeek.MONDAY);
    LocalDate sunday = monday.plusDays(6);

    return monday
        .datesUntil(sunday.plusDays(1))
        .map(
            date ->
                new LastWeekDayDetailResponse(
                    date.format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.ENGLISH)),
                    date,
                    sessionMap.getOrDefault(date, List.of())))
        .toList();
  }

  public Map<LocalDate, List<SessionIntervalDto>> toSessionIntervalDtoMap(
      List<SessionHistory> sessions) {
    return sessions.stream()
        .map(s -> new SessionIntervalDto(s.getStart(), s.getStart().plusSeconds(s.getSeconds())))
        .flatMap(this::splitSessionAtMidnight)
        .map(s -> Map.entry(s.startTime().toLocalDate(), s))
        .collect(
            Collectors.groupingBy(
                Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  public Stream<SessionIntervalDto> splitSessionAtMidnight(SessionIntervalDto session) {
    if (!LocalDate.from(session.startTime()).equals(LocalDate.from(session.endTime()))) {
      return Stream.of(
          new SessionIntervalDto(
              session.startTime(), session.startTime().withHour(23).withMinute(59)),
          new SessionIntervalDto(session.endTime().withHour(0).withMinute(0), session.endTime()));
    } else {
      return Stream.of(session);
    }
  }

  public List<AverageDayDetailResponse> toAverageDayDetailResponseList(
      List<SessionHistory> sessions, long timeframeInDays) {

    int[] contributingDays = this.calculateContributingDays(timeframeInDays);
    TimeBucket[][] buckets = toTimeBuckets(sessions);

    List<AverageDayDetailResponse> res = new ArrayList<>();

    for (int i = 0; i < buckets.length; i++) {
      res.add(
          new AverageDayDetailResponse(
              DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
              i,
              this.toAverageTimeBucketResponseList(buckets[i], contributingDays[i]),
              contributingDays[i]));
    }

    return res;
  }

  public int[] calculateContributingDays(long totalDays) {
    int[] counts = new int[7];
    LocalDate date = LocalDate.now(clock).minusDays(1);

    for (int i = 0; i < totalDays; i++) {
      int index = date.getDayOfWeek().getValue() - 1;
      counts[index]++;
      date = date.minusDays(1);
    }

    return counts;
  }

  public TimeBucket[][] toTimeBuckets(List<SessionHistory> sessions) {
    TimeBucket[][] buckets = this.initializeWeekdayBucketsWithIndexAndLocalTime();
    return this.populateWithTotalMinutesAndContributingSessions(buckets, sessions);
  }

  private TimeBucket[][] populateWithTotalMinutesAndContributingSessions(
      TimeBucket[][] buckets, List<SessionHistory> sessions) {
    List<SessionIntervalDto> intervals =
        sessions.stream()
            .map(
                s -> new SessionIntervalDto(s.getStart(), s.getStart().plusSeconds(s.getSeconds())))
            .toList();

    int timeBucketSizeInMinutes = 24 * 60 / TIME_BUCKETS_PER_DAY;

    for (SessionIntervalDto i : intervals) {
      int weekday = i.startTime().getDayOfWeek().getValue() - 1;
      int startBucket = i.startTime().get(ChronoField.MINUTE_OF_DAY) / timeBucketSizeInMinutes;

      do {
        int bucketStartMinute =
            buckets[weekday][startBucket].getStartTime().get(ChronoField.MINUTE_OF_DAY);
        int bucketEndMinute = bucketStartMinute + timeBucketSizeInMinutes;

        int endMinuteOfDay = i.endTime().get(ChronoField.MINUTE_OF_DAY);
        if (weekday != i.endTime().getDayOfWeek().getValue() - 1) {
          endMinuteOfDay = 24 * 60;
        }
        int startMinuteOfDay = i.startTime().get(ChronoField.MINUTE_OF_DAY);
        if (weekday != i.startTime().getDayOfWeek().getValue() - 1) {
          startMinuteOfDay = 0;
        }

        int overlap =
            Math.min(endMinuteOfDay, bucketEndMinute)
                - Math.max(startMinuteOfDay, bucketStartMinute);

        if (overlap > 0) {
          buckets[weekday][startBucket].addMinutes(overlap);
          buckets[weekday][startBucket].incrementContributingSessions();
        }

        startBucket = (startBucket + 1) % buckets[0].length;
        if (startBucket == 0) {
          weekday = (weekday + 1) % 7;
        }

      } while (i.endTime().toLocalTime().isAfter(buckets[weekday][startBucket].getStartTime())
          && weekday <= i.endTime().getDayOfWeek().getValue() - 1);
    }
    return buckets;
  }

  private TimeBucket[][] initializeWeekdayBucketsWithIndexAndLocalTime() {
    int timeBucketSizeInMinutes = 24 * 60 / TIME_BUCKETS_PER_DAY;
    TimeBucket[][] buckets = new TimeBucket[7][TIME_BUCKETS_PER_DAY];
    LocalTime timestamp = LocalTime.MIDNIGHT;

    for (int i = 0; i < buckets.length; i++) {
      for (int j = 0; j < TIME_BUCKETS_PER_DAY; j++) {
        buckets[i][j] = new TimeBucket(j, timestamp);
        timestamp = timestamp.plusMinutes(timeBucketSizeInMinutes);
      }
    }
    return buckets;
  }

  private List<AverageTimeBucketResponse> toAverageTimeBucketResponseList(
      TimeBucket[] buckets, int contributingDays) {
    return Stream.of(buckets)
        .map(
            b ->
                new AverageTimeBucketResponse(
                    this.timestampToString(b.getStartTime()),
                    (double) b.getTotalMinutes() / contributingDays,
                    b.getContributingSessions()))
        .toList();
  }

  public String timestampToString(LocalTime timestamp) {
    int timeBucketSizeInMinutes = 24 * 60 / TIME_BUCKETS_PER_DAY;
    return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))
        + "-"
        + timestamp
            .plusMinutes(timeBucketSizeInMinutes)
            .format(DateTimeFormatter.ofPattern("HH:mm"));
  }
}

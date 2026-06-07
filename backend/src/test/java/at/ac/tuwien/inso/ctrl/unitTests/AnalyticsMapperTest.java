package at.ac.tuwien.inso.ctrl.unitTests;

import static org.assertj.core.api.Assertions.assertThat;

import at.ac.tuwien.inso.ctrl.session.dto.AverageDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.AverageDayResponse;
import at.ac.tuwien.inso.ctrl.session.dto.AverageTimeBucketResponse;
import at.ac.tuwien.inso.ctrl.session.dto.LastWeekDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.SessionIntervalDto;
import at.ac.tuwien.inso.ctrl.session.entity.DayOfWeekAverage;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.mapper.AnalyticsMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AnalyticsMapperTest {

  private static final Clock CLOCK =
      Clock.fixed(LocalDateTime.of(2026, 5, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  // "today" = Friday 2026-05-01, so "last 7 days" = 2026-04-24 (Fri) through
  // 2026-04-30 (Thu)

  private AnalyticsMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new AnalyticsMapper(CLOCK);
  }

  private SessionHistory session(LocalDateTime start, int minutes) {
    SessionHistory s = new SessionHistory();
    s.setStart(start);
    s.setMinutes((long) minutes);
    return s;
  }

  @Nested
  class ToLastWeekDayDetailResponseList {

    @Test
    void shouldReturnExactlySevenEntries() {
      List<LastWeekDayDetailResponse> result = mapper.toLastWeekDayDetailResponseList(List.of());
      assertThat(result).hasSize(7);
    }

    @Test
    void shouldCoverCorrectDateRange() {
      List<LastWeekDayDetailResponse> result = mapper.toLastWeekDayDetailResponseList(List.of());
      List<LocalDate> expected =
          List.of(
              LocalDate.of(2026, 4, 24),
              LocalDate.of(2026, 4, 25),
              LocalDate.of(2026, 4, 26),
              LocalDate.of(2026, 4, 27),
              LocalDate.of(2026, 4, 28),
              LocalDate.of(2026, 4, 29),
              LocalDate.of(2026, 4, 30));
      for (int i = 0; i < 7; i++) {
        assertThat(result.get(i).date()).isEqualTo(expected.get(i));
      }
    }

    @Test
    void shouldReturnEmptyIntervalsForDaysWithNoSessions() {
      List<LastWeekDayDetailResponse> result =
          mapper.toLastWeekDayDetailResponseList(
              List.of(session(LocalDateTime.of(2026, 4, 27, 10, 0), 30)));
      assertThat(
              result.stream()
                  .filter(d -> !d.date().equals(LocalDate.of(2026, 4, 27)))
                  .allMatch(d -> d.sessions().isEmpty()))
          .isTrue();
    }

    @Test
    void shouldNotSplitSessionWithinSameDay() {
      List<LastWeekDayDetailResponse> result =
          mapper.toLastWeekDayDetailResponseList(
              List.of(session(LocalDateTime.of(2026, 4, 27, 10, 0), 30)));
      LastWeekDayDetailResponse day =
          result.stream()
              .filter(d -> d.date().equals(LocalDate.of(2026, 4, 27)))
              .findFirst()
              .orElseThrow();
      assertThat(day.sessions()).hasSize(1);
      assertThat(day.sessions().get(0).startTime()).isEqualTo(LocalDateTime.of(2026, 4, 27, 10, 0));
      assertThat(day.sessions().get(0).endTime()).isEqualTo(LocalDateTime.of(2026, 4, 27, 10, 30));
    }

    @Test
    void shouldSplitSessionCrossingMidnight() {
      List<LastWeekDayDetailResponse> result =
          mapper.toLastWeekDayDetailResponseList(
              List.of(session(LocalDateTime.of(2026, 4, 27, 23, 30), 60)));
      LastWeekDayDetailResponse apr27 =
          result.stream()
              .filter(d -> d.date().equals(LocalDate.of(2026, 4, 27)))
              .findFirst()
              .orElseThrow();
      assertThat(apr27.sessions()).hasSize(1);
      assertThat(apr27.sessions().get(0).startTime())
          .isEqualTo(LocalDateTime.of(2026, 4, 27, 23, 30));
      assertThat(apr27.sessions().get(0).endTime()).isEqualTo(LocalDateTime.of(2026, 4, 28, 0, 0));

      LastWeekDayDetailResponse apr28 =
          result.stream()
              .filter(d -> d.date().equals(LocalDate.of(2026, 4, 28)))
              .findFirst()
              .orElseThrow();
      assertThat(apr28.sessions()).hasSize(1);
      assertThat(apr28.sessions().get(0).startTime())
          .isEqualTo(LocalDateTime.of(2026, 4, 28, 0, 0));
      assertThat(apr28.sessions().get(0).endTime()).isEqualTo(LocalDateTime.of(2026, 4, 28, 0, 30));
    }

    @Test
    void shouldPlaceSplitSessionPartsOnCorrectDates() {
      List<LastWeekDayDetailResponse> result =
          mapper.toLastWeekDayDetailResponseList(
              List.of(session(LocalDateTime.of(2026, 4, 27, 23, 30), 60)));
      LastWeekDayDetailResponse apr27 =
          result.stream()
              .filter(d -> d.date().equals(LocalDate.of(2026, 4, 27)))
              .findFirst()
              .orElseThrow();
      assertThat(apr27.sessions().get(0).startTime())
          .isEqualTo(LocalDateTime.of(2026, 4, 27, 23, 30));
      assertThat(apr27.sessions().get(0).endTime()).isEqualTo(LocalDateTime.of(2026, 4, 28, 0, 0));

      LastWeekDayDetailResponse apr28 =
          result.stream()
              .filter(d -> d.date().equals(LocalDate.of(2026, 4, 28)))
              .findFirst()
              .orElseThrow();
      assertThat(apr28.sessions().get(0).startTime())
          .isEqualTo(LocalDateTime.of(2026, 4, 28, 0, 0));
      assertThat(apr28.sessions().get(0).endTime()).isEqualTo(LocalDateTime.of(2026, 4, 28, 0, 30));
    }
  }

  @Nested
  class SplitSessionAtMidnight {

    @Test
    void shouldReturnSingleIntervalForSameDaySession() {
      List<SessionIntervalDto> result =
          mapper
              .splitSessionAtMidnight(
                  new SessionIntervalDto(
                      LocalDateTime.of(2026, 5, 1, 10, 0), LocalDateTime.of(2026, 5, 1, 10, 45)))
              .toList();
      assertThat(result).hasSize(1);
    }

    @Test
    void shouldReturnTwoIntervalsForCrossMidnightSession() {
      List<SessionIntervalDto> result =
          mapper
              .splitSessionAtMidnight(
                  new SessionIntervalDto(
                      LocalDateTime.of(2026, 4, 30, 23, 30), LocalDateTime.of(2026, 5, 1, 0, 30)))
              .toList();
      assertThat(result).hasSize(2);
    }

    @Test
    void shouldEndFirstIntervalAtMidnight() {
      List<SessionIntervalDto> result =
          mapper
              .splitSessionAtMidnight(
                  new SessionIntervalDto(
                      LocalDateTime.of(2026, 4, 30, 23, 30), LocalDateTime.of(2026, 5, 1, 0, 30)))
              .toList();
      assertThat(result.get(0).endTime()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
    }

    @Test
    void shouldStartSecondIntervalAtMidnight() {
      List<SessionIntervalDto> result =
          mapper
              .splitSessionAtMidnight(
                  new SessionIntervalDto(
                      LocalDateTime.of(2026, 4, 30, 23, 30), LocalDateTime.of(2026, 5, 1, 0, 30)))
              .toList();
      assertThat(result.get(1).startTime()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
    }
  }

  @Nested
  class CalculateContributingDays {

    @Test
    void shouldReturnArrayOfLengthSeven() {
      int[] result = mapper.calculateContributingDays(1);
      assertThat(result).hasSize(7);
    }

    @Test
    void shouldReturnEightForEachDayWithFullEightWeekWindow() {
      int[] result = mapper.calculateContributingDays(56);
      assertThat(result).containsExactly(8, 8, 8, 8, 8, 8, 8);
    }

    @Test
    void shouldCountCorrectlyForPartialWindow() {
      int[] result = mapper.calculateContributingDays(3);
      assertThat(result[1]).isEqualTo(1); // Tuesday
      assertThat(result[2]).isEqualTo(1); // Wednesday
      assertThat(result[3]).isEqualTo(1); // Thursday
      assertThat(result[0]).isZero();
      assertThat(result[4]).isZero();
      assertThat(result[5]).isZero();
      assertThat(result[6]).isZero();
    }

    @Test
    void shouldExcludeToday() {
      int[] result = mapper.calculateContributingDays(1);
      assertThat(result[3]).isEqualTo(1); // Thursday (yesterday)
      assertThat(result[0]).isZero();
      assertThat(result[1]).isZero();
      assertThat(result[2]).isZero();
      assertThat(result[4]).isZero();
      assertThat(result[5]).isZero();
      assertThat(result[6]).isZero();
    }
  }

  @Nested
  class ToTimeBuckets {

    @Test
    void shouldReturnAllZeroBucketsForEmptySessionList() {
      var buckets = mapper.toTimeBuckets(List.of());
      for (int day = 0; day < 7; day++) {
        for (int bucket = 0; bucket < 96; bucket++) {
          assertThat(buckets[day][bucket].getTotalMinutes()).isZero();
        }
      }
    }

    @Test
    void shouldAddMinutesToCorrectBucketForSessionWithinOneBucket() {
      var buckets =
          mapper.toTimeBuckets(List.of(session(LocalDateTime.of(2026, 4, 27, 10, 0), 10)));
      assertThat(buckets[0][40].getTotalMinutes()).isEqualTo(10);
    }

    @Test
    void shouldDistributeMinutesAcrossTwoBucketsWhenSessionSpansBucketBoundary() {
      var buckets =
          mapper.toTimeBuckets(List.of(session(LocalDateTime.of(2026, 4, 27, 10, 10), 20)));
      assertThat(buckets[0][40].getTotalMinutes()).isEqualTo(5);
      assertThat(buckets[0][41].getTotalMinutes()).isEqualTo(15);
    }

    @Test
    void shouldIncrementContributingSessionsOncePerOverlappingBucket() {
      var buckets =
          mapper.toTimeBuckets(List.of(session(LocalDateTime.of(2026, 4, 27, 10, 10), 20)));
      assertThat(buckets[0][40].getContributingSessions()).isEqualTo(1);
      assertThat(buckets[0][41].getContributingSessions()).isEqualTo(1);
    }

    @Test
    void shouldPlaceCrossMidnightSessionInCorrectWeekdayBuckets() {
      var buckets =
          mapper.toTimeBuckets(List.of(session(LocalDateTime.of(2026, 4, 27, 23, 50), 20)));
      assertThat(buckets[0][95].getTotalMinutes()).isEqualTo(10);
      assertThat(buckets[1][0].getTotalMinutes()).isEqualTo(10);
    }
  }

  @Nested
  class ToAverageDayDetailResponseList {

    @Test
    void shouldReturnSevenEntries() {
      List<AverageDayDetailResponse> result = mapper.toAverageDayDetailResponseList(List.of(), 56);
      assertThat(result).hasSize(7);
    }

    @Test
    void shouldLabelEntriesWithCorrectDayOfWeekNames() {
      List<AverageDayDetailResponse> result = mapper.toAverageDayDetailResponseList(List.of(), 56);
      List<Integer> labels = result.stream().map(AverageDayDetailResponse::dayOfWeek).toList();
      assertThat(labels).containsExactly(0, 1, 2, 3, 4, 5, 6);
    }

    @Test
    void shouldSetDaysInPeriodFromContributingDays() {
      List<AverageDayDetailResponse> result = mapper.toAverageDayDetailResponseList(List.of(), 56);
      assertThat(result).allMatch(d -> d.daysInPeriod() == 8);
    }

    @Test
    void shouldAverageMinutesNotSumThem() {
      List<SessionHistory> sessions =
          List.of(
              session(LocalDateTime.of(2026, 4, 23, 10, 0), 15),
              session(LocalDateTime.of(2026, 4, 30, 10, 0), 15));
      List<AverageDayDetailResponse> result = mapper.toAverageDayDetailResponseList(sessions, 9);
      AverageDayDetailResponse thursday =
          result.stream().filter(d -> d.dayOfWeek() == 3).findFirst().orElseThrow();
      AverageTimeBucketResponse bucket = thursday.buckets().get(40); // 10:00 bucket
      assertThat(bucket.averageTime()).isEqualTo(15.0);
    }
  }

  @Nested
  class ToAverageDayResponse {

    @Test
    void shouldMapDayOfWeekAverageToAverageDayResponse() {
      DayOfWeekAverage day =
          new DayOfWeekAverage() {
            @Override
            public Integer getDayOfWeek() {
              return 4; // Thursday
            }

            @Override
            public Double getAvgMinutes() {
              return 7.5;
            }

            @Override
            public Double getAvgSessionCount() {
              return 2.5;
            }

            @Override
            public Integer getDaysInPeriod() {
              return 8;
            }
          };

      AverageDayResponse result = mapper.toAverageDayResponse(day);
      assertThat(result.dayOfWeek()).isEqualTo(3);
      assertThat(result.avgTotalMinutes()).isEqualTo(7.5);
      assertThat(result.avgSessionCount()).isEqualTo(2.5);
      assertThat(result.daysInPeriod()).isEqualTo(8);
    }

    @Test
    void shouldPreserveFractionalAverages() {
      DayOfWeekAverage day =
          new DayOfWeekAverage() {
            @Override
            public Integer getDayOfWeek() {
              return 1;
            }

            @Override
            public Double getAvgMinutes() {
              return 0.75;
            }

            @Override
            public Double getAvgSessionCount() {
              return 0.25;
            }

            @Override
            public Integer getDaysInPeriod() {
              return 1;
            }
          };

      AverageDayResponse result = mapper.toAverageDayResponse(day);
      assertThat(result.avgTotalMinutes()).isEqualTo(0.75);
      assertThat(result.avgSessionCount()).isEqualTo(0.25);
    }
  }

  @Nested
  class TimestampToString {

    @Test
    void shouldFormatMidnightCorrectly() {
      assertThat(mapper.timestampToString(LocalTime.of(0, 0))).isEqualTo("00:00-00:15");
    }

    @Test
    void shouldFormatNormalTimestampCorrectly() {
      assertThat(mapper.timestampToString(LocalTime.of(9, 30))).isEqualTo("09:30-09:45");
    }

    @Test
    void shouldWrapAroundForLastBucket() {
      assertThat(mapper.timestampToString(LocalTime.of(23, 45))).isEqualTo("23:45-00:00");
    }
  }
}

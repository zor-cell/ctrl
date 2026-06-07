package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
@Import(TestConfig.class)
public class SessionRepositoryTest {

  @Autowired private SessionRepository sessionRepository;

  @Autowired private UserRepository userRepository;

  private User targetUser;
  private User otherUser;

  private static final LocalDate TODAY = LocalDate.of(2024, 3, 6); // Wednesday
  private static final LocalDate WEEK_AGO = TODAY.minusDays(7); // 2024-02-28
  private static final LocalDate FIFTY_SIX_DAYS_AGO = TODAY.minusDays(56); // 2024-01-09

  @BeforeEach
  void setUp() {
    targetUser =
        userRepository.save(
            User.builder()
                .username("targetuser")
                .password("password")
                .email("target@example.com")
                .name("Target User")
                .build());
    otherUser =
        userRepository.save(
            User.builder()
                .username("otheruser")
                .password("password")
                .email("other@example.com")
                .name("Other User")
                .build());
  }

  @AfterEach
  void tearDown() {
    sessionRepository.deleteAll();
    userRepository.deleteAll();
  }

  private SessionHistory session(User user, LocalDateTime start, int minutes) {
    SessionHistory s = new SessionHistory();
    s.setUser(user);
    s.setStart(start);
    s.setMinutes((long) minutes);
    return sessionRepository.save(s);
  }

  private SessionHistory sessionNullMinutes(User user, LocalDateTime start) {
    SessionHistory s = new SessionHistory();
    s.setUser(user);
    s.setStart(start);
    s.setMinutes(null);
    return sessionRepository.save(s);
  }

  @Nested
  class FindLastWeekSummary {

    @Test
    void shouldReturnExactlySevenRowsWhenNoSessionsExist() {
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      assertThat(result).hasSize(7);
    }

    @Test
    void shouldReturnRowsInAscendingDateOrder() {
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      assertThat(result).hasSize(7);
      IntStream.range(0, 6)
          .forEach(i -> assertThat(result.get(i).getDay()).isBefore(result.get(i + 1).getDay()));
    }

    @Test
    void shouldReturnZeroTotalMinutesAndZeroSessionCountForDayWithNoSessions() {
      session(targetUser, LocalDateTime.of(2024, 2, 29, 12, 0), 30);
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      var dayWithNoSession =
          result.stream().filter(r -> !r.getDay().equals(LocalDate.of(2024, 2, 29))).toList();
      assertAll(
          () -> assertThat(dayWithNoSession).hasSize(6),
          () ->
              assertThat(dayWithNoSession)
                  .allMatch(r -> r.getTotalMinutes() == 0L && r.getSessionCount() == 0L));
    }

    @Test
    void shouldSumMinutesForDayWithMultipleSessions() {
      LocalDate targetDay = WEEK_AGO.plusDays(1); // 2024-02-29
      session(targetUser, targetDay.atTime(10, 0), 10);
      session(targetUser, targetDay.atTime(11, 0), 20);
      session(targetUser, targetDay.atTime(12, 0), 30);
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      var dayResult =
          result.stream().filter(r -> r.getDay().equals(targetDay)).findFirst().orElseThrow();
      assertAll(
          () -> assertThat(dayResult.getTotalMinutes()).isEqualTo(60L),
          () -> assertThat(dayResult.getSessionCount()).isEqualTo(3L));
    }

    @Test
    void shouldIncludeSessionOnFirstDayOfWindow() {
      session(targetUser, WEEK_AGO.atTime(12, 0), 30);
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      var firstDay =
          result.stream().filter(r -> r.getDay().equals(WEEK_AGO)).findFirst().orElseThrow();
      assertThat(firstDay.getSessionCount()).isEqualTo(1L);
    }

    @Test
    void shouldExcludeSessionOnToday() {
      session(targetUser, TODAY.atTime(12, 0), 30);
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getSessionCount() == 0L);
    }

    @Test
    void shouldExcludeSessionOneSecondBeforeWindow() {
      session(targetUser, WEEK_AGO.atStartOfDay().minusSeconds(1), 30);
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getSessionCount() == 0L);
    }

    @Test
    void shouldExcludeSessionsWithNullMinutes() {
      sessionNullMinutes(targetUser, WEEK_AGO.plusDays(1).atTime(12, 0));
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      var dayResult =
          result.stream()
              .filter(r -> r.getDay().equals(WEEK_AGO.plusDays(1)))
              .findFirst()
              .orElseThrow();
      assertThat(dayResult.getSessionCount()).isEqualTo(0L);
    }

    @Test
    void shouldNotIncludeOtherUsersSessions() {
      IntStream.range(0, 7)
          .forEach(i -> session(otherUser, WEEK_AGO.plusDays(i).atTime(12, 0), 30));
      var result = sessionRepository.findLastWeekSummary(targetUser.getId(), WEEK_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getSessionCount() == 0L);
    }
  }

  @Nested
  class FindDayOfWeekAverages {

    @Test
    void shouldReturnExactlySevenRows() {
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      assertThat(result).hasSize(7);
    }

    @Test
    void shouldReturnRowsOrderedByIsoWeekday() {
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      assertThat(result).hasSize(7);
      IntStream.range(0, 7).forEach(i -> assertThat(result.get(i).getDayOfWeek()).isEqualTo(i + 1));
    }

    @Test
    void shouldReturnZeroAvgMinutesNotNullForWeekdayWithNoSessions() {
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getAvgMinutes() != null && r.getAvgMinutes() == 0.0);
    }

    @Test
    void shouldReturnCorrectDaysInPeriodForFullEightWeekWindow() {
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getDaysInPeriod() == 8);
    }

    @Test
    void shouldAverageAcrossCalendarDaysNotSumThem() {
      // Two Wednesdays within the window, each with a 60-minute session
      session(targetUser, TODAY.minusDays(7).atTime(10, 0), 60); // 2024-02-28
      session(targetUser, TODAY.minusDays(14).atTime(10, 0), 60); // 2024-02-21
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      var wednesday = result.stream().filter(r -> r.getDayOfWeek() == 3).findFirst().orElseThrow();
      // 120 minutes across 8 Wednesdays = 15.0 average
      assertThat(wednesday.getAvgMinutes()).isEqualTo(15.0);
    }

    @Test
    void shouldSetDaysInPeriodToWindowOccurrencesNotSessionOccurrences() {
      session(targetUser, TODAY.minusDays(7).atTime(10, 0), 30); // one Wednesday only
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      var wednesday = result.stream().filter(r -> r.getDayOfWeek() == 3).findFirst().orElseThrow();
      assertThat(wednesday.getDaysInPeriod()).isEqualTo(8);
    }

    @Test
    void shouldIncludeSessionOnFirstDayOfWindow() {
      session(targetUser, FIFTY_SIX_DAYS_AGO.atTime(12, 0), 30); // 2024-01-10 is Wednesday
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      var wednesday = result.stream().filter(r -> r.getDayOfWeek() == 3).findFirst().orElseThrow();
      assertThat(wednesday.getAvgMinutes()).isGreaterThan(0.0);
    }

    @Test
    void shouldExcludeSessionOnToday() {
      session(targetUser, TODAY.atTime(12, 0), 30);
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getAvgMinutes() == 0.0);
    }

    @Test
    void shouldExcludeSessionOneSecondBeforeWindow() {
      session(targetUser, FIFTY_SIX_DAYS_AGO.atStartOfDay().minusSeconds(1), 30);
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getAvgMinutes() == 0.0);
    }

    @Test
    void shouldExcludeSessionsWithNullMinutes() {
      sessionNullMinutes(targetUser, TODAY.minusDays(7).atTime(12, 0));
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getAvgMinutes() == 0.0);
    }

    @Test
    void shouldNotIncludeOtherUsersData() {
      // Seed otherUser with sessions on every day of the 56-day window
      IntStream.range(0, 56)
          .forEach(i -> session(otherUser, FIFTY_SIX_DAYS_AGO.plusDays(i).atTime(12, 0), 30));
      var result =
          sessionRepository.findDayOfWeekAverages(targetUser.getId(), FIFTY_SIX_DAYS_AGO, TODAY);
      assertThat(result).allMatch(r -> r.getAvgMinutes() == 0.0);
    }
  }

  @Nested
  class FindSessionsForLastWeek {

    @Test
    void shouldReturnSessionWithinWindow() {
      var session = session(targetUser, WEEK_AGO.plusDays(3).atTime(10, 0), 30);
      var result =
          sessionRepository.findSessionsForLastWeek(
              targetUser, WEEK_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).extracting(SessionHistory::getSessionId).contains(session.getSessionId());
    }

    @Test
    void shouldIncludeSessionAtExactStartBoundary() {
      var session = session(targetUser, WEEK_AGO.atStartOfDay(), 30);
      var result =
          sessionRepository.findSessionsForLastWeek(
              targetUser, WEEK_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).extracting(SessionHistory::getSessionId).contains(session.getSessionId());
    }

    @Test
    void shouldExcludeSessionOneSecondBeforeStartBoundary() {
      session(targetUser, WEEK_AGO.atStartOfDay().minusSeconds(1), 30);
      var result =
          sessionRepository.findSessionsForLastWeek(
              targetUser, WEEK_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }

    @Test
    void shouldExcludeSessionAtExactEndBoundary() {
      session(targetUser, TODAY.atStartOfDay(), 30);
      var result =
          sessionRepository.findSessionsForLastWeek(
              targetUser, WEEK_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }

    @Test
    void shouldIncludeSessionOneSecondBeforeEndBoundary() {
      var session = session(targetUser, TODAY.atStartOfDay().minusSeconds(1), 30);
      var result =
          sessionRepository.findSessionsForLastWeek(
              targetUser, WEEK_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).extracting(SessionHistory::getSessionId).contains(session.getSessionId());
    }

    @Test
    void shouldReturnEmptyListWhenNoSessionsExist() {
      var result =
          sessionRepository.findSessionsForLastWeek(
              targetUser, WEEK_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnMultipleSessionsOnSameDay() {
      var s1 = session(targetUser, WEEK_AGO.plusDays(1).atTime(10, 0), 10);
      var s2 = session(targetUser, WEEK_AGO.plusDays(1).atTime(11, 0), 20);
      var s3 = session(targetUser, WEEK_AGO.plusDays(1).atTime(12, 0), 30);
      var result =
          sessionRepository.findSessionsForLastWeek(
              targetUser, WEEK_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).hasSize(3);
      assertThat(result)
          .extracting(SessionHistory::getSessionId)
          .containsExactlyInAnyOrder(s1.getSessionId(), s2.getSessionId(), s3.getSessionId());
    }

    @Test
    void shouldNotReturnOtherUsersSessions() {
      session(otherUser, WEEK_AGO.plusDays(2).atTime(10, 0), 30);
      var result =
          sessionRepository.findSessionsForLastWeek(
              targetUser, WEEK_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }
  }

  @Nested
  class FindSessionsForPeriod {

    @Test
    void shouldReturnSessionWithinWindow() {
      var session = session(targetUser, TODAY.minusDays(3).atTime(12, 0), 30);
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).extracting(SessionHistory::getSessionId).contains(session.getSessionId());
    }

    @Test
    void shouldIncludeSessionAtExactStartBoundary() {
      var session = session(targetUser, FIFTY_SIX_DAYS_AGO.atStartOfDay(), 30);
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).extracting(SessionHistory::getSessionId).contains(session.getSessionId());
    }

    @Test
    void shouldExcludeSessionOneSecondBeforeStartBoundary() {
      session(targetUser, FIFTY_SIX_DAYS_AGO.atStartOfDay().minusSeconds(1), 30);
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }

    @Test
    void shouldExcludeSessionAtExactEndBoundary() {
      session(targetUser, TODAY.atStartOfDay(), 30);
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }

    @Test
    void shouldIncludeSessionOneSecondBeforeEndBoundary() {
      var session = session(targetUser, TODAY.atStartOfDay().minusSeconds(1), 30);
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).extracting(SessionHistory::getSessionId).contains(session.getSessionId());
    }

    @Test
    void shouldExcludeSessionsWithNullMinutes() {
      sessionNullMinutes(targetUser, TODAY.minusDays(10).atTime(12, 0));
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }

    @Test
    void shouldNotReturnOtherUsersData() {
      session(otherUser, TODAY.minusDays(10).atTime(12, 0), 30);
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoSessionsExist() {
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnSessionsOrderedByStartAscending() {
      var s1 = session(targetUser, TODAY.minusDays(10).atTime(12, 0), 30);
      var s2 = session(targetUser, TODAY.minusDays(5).atTime(10, 0), 20);
      var s3 = session(targetUser, TODAY.minusDays(1).atTime(8, 0), 10);
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result)
          .hasSize(3)
          .isSortedAccordingTo(Comparator.comparing(SessionHistory::getStart));
      assertThat(result)
          .extracting(SessionHistory::getSessionId)
          .containsExactly(s1.getSessionId(), s2.getSessionId(), s3.getSessionId());
    }

    @Test
    void shouldReturnSessionsForAllWeekdays() {
      // Insert one session on each day of the week within the window
      var mon = session(targetUser, LocalDateTime.of(2024, 2, 26, 10, 0), 10); // Monday
      var tue = session(targetUser, LocalDateTime.of(2024, 2, 27, 10, 0), 10); // Tuesday
      var wed = session(targetUser, LocalDateTime.of(2024, 2, 28, 10, 0), 10); // Wednesday
      var thu = session(targetUser, LocalDateTime.of(2024, 2, 29, 10, 0), 10); // Thursday
      var fri = session(targetUser, LocalDateTime.of(2024, 3, 1, 10, 0), 10); // Friday
      var sat = session(targetUser, LocalDateTime.of(2024, 3, 2, 10, 0), 10); // Saturday
      var sun = session(targetUser, LocalDateTime.of(2024, 3, 3, 10, 0), 10); // Sunday
      var result =
          sessionRepository.findSessionsForPeriod(
              targetUser.getId(), FIFTY_SIX_DAYS_AGO.atStartOfDay(), TODAY.atStartOfDay());
      assertThat(result).hasSize(7);
      assertThat(result)
          .extracting(SessionHistory::getSessionId)
          .containsExactlyInAnyOrder(
              mon.getSessionId(),
              tue.getSessionId(),
              wed.getSessionId(),
              thu.getSessionId(),
              fri.getSessionId(),
              sat.getSessionId(),
              sun.getSessionId());
    }
  }
}

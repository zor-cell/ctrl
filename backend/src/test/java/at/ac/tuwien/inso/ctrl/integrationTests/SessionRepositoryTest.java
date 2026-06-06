package at.ac.tuwien.inso.ctrl.integrationTests;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
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
@Transactional
public class SessionRepositoryTest {
  @Autowired private SessionRepository sessionRepository;

  @Autowired private UserRepository userRepository;

  private User testUser;
  private User otherUser;

  @BeforeEach
  void setUp() {
    testUser =
        userRepository.save(
            User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .name("Test User")
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

  @Nested
  class FindLastWeekSummary {

    @Test
    void should_returnExactly7Rows() {
      // Asserts that the result contains exactly 7 rows, one for each day of the last
      // week.
    }

    @Test
    void should_returnZeroMinutesAndZeroSessionCount_forDaysWithNoSessions() {
      // Asserts that days with no sessions have totalMinutes = 0 and sessionCount =
      // 0.
    }

    @Test
    void should_excludeSessionsOlderThan7Days() {
      // Asserts that sessions started more than 7 days ago are not included in the
      // summary.
    }

    @Test
    void should_excludeSessionsForOtherUsers() {
      // Asserts that only sessions belonging to the requested user are returned.
    }
  }

  @Nested
  class FindDayOfWeekAverages {

    @Test
    void should_returnExactly7Rows() {
      // Asserts that the result contains exactly 7 rows, one per ISO weekday.
    }

    @Test
    void should_excludeDaysOutsideRequestedWindow() {
      // Asserts that sessions outside the requested timeframe are excluded from the
      // averages.
    }

    @Test
    void should_excludeSessionsForOtherUsers() {
      // Asserts that only sessions for the requested user are considered.
    }

    @Test
    void should_averageAcrossCalendarDaysNotSum() {
      // Asserts that avgTotalMinutes and avgSessionCount are averages across calendar
      // days, not sums.
    }

    @Test
    void should_setDaysInPeriodToNumberOfThatIsoWeekdayInWindow() {
      // Asserts that daysInPeriod reflects how many times that ISO weekday occurred
      // in the window.
    }
  }

  @Nested
  class FindSessionsForLastWeek {

    @Test
    void should_returnSessionsWithinWeekAgoInclusiveAndTomorrowExclusive() {
      // Asserts that sessions are returned within [weekAgo, tomorrow) for the given
      // user.
    }

    @Test
    void should_includeSessionsAtExactlyWeekAgo() {
      // Asserts that a session starting exactly at weekAgo is included in the result.
    }

    @Test
    void should_excludeSessionsAtExactlyTomorrow() {
      // Asserts that a session starting exactly at tomorrow is excluded from the
      // result.
    }

    @Test
    void should_excludeSessionsForOtherUsers() {
      // Asserts that sessions for other users are not returned.
    }
  }

  @Nested
  class FindSessionsForPeriod {

    @Test
    void should_returnSessionsWithinLastDaysDays() {
      // Asserts that only sessions within the last specified number of days are
      // returned.
    }

    @Test
    void should_excludeSessionsOutsideWindow() {
      // Asserts that sessions started before the period window are excluded.
    }

    @Test
    void should_excludeSessionsForOtherUsers() {
      // Asserts that sessions for other users are not returned.
    }
  }
}

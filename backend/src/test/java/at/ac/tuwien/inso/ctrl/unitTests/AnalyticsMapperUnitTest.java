package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import at.ac.tuwien.inso.ctrl.session.dto.LastWeekDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.SessionIntervalDto;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.mapper.AnalyticsMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
public class AnalyticsMapperUnitTest {
  private static final LocalDate TEST_DATE = LocalDate.of(2026, 1, 1);
  private static final Clock FIXED_CLOCK =
      Clock.fixed(TEST_DATE.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private AnalyticsMapper mapper;
  private SessionHistory session1;
  private SessionHistory session2;
  private SessionHistory session3;
  private SessionHistory session4;

  @BeforeEach
  void setUp() {
    this.mapper = new AnalyticsMapper(FIXED_CLOCK);
    UserDetails user = User.builder().username("user").build();

    LocalDateTime time2025_12_24_23_47 = LocalDateTime.of(2025, 12, 24, 23, 47);
    long min1 = 31L;
    session1 = SessionHistory.builder().seconds(min1 * 60).start(time2025_12_24_23_47).build();

    LocalDateTime time2025_12_27_22_15 = LocalDateTime.of(2025, 12, 27, 22, 15);
    long min2 = 231L;
    session2 = SessionHistory.builder().seconds(min2 * 60).start(time2025_12_27_22_15).build();

    LocalDateTime time2025_12_31_23_23 = LocalDateTime.of(2025, 12, 31, 23, 23);
    long min3 = 62L;
    session3 = SessionHistory.builder().seconds(min3 * 60).start(time2025_12_31_23_23).build();

    LocalDateTime time2025_12_27_12_23 = LocalDateTime.of(2025, 12, 27, 12, 23);
    long min4 = 29L;
    session4 = SessionHistory.builder().seconds(min4 * 60).start(time2025_12_27_12_23).build();
  }

  // ********************************************************************************
  // lastWeekResponse
  // ********************************************************************************

  // ********************************************************************************
  // lastWeekDetailResponse
  // ********************************************************************************

  // toLastWeekDayDetailResponseList
  @Test
  public void toLastWeekDayDetailResponseList_withSingleSession_returnsWeekOfThatSession() {
    List<LastWeekDayDetailResponse> res =
        this.mapper.toLastWeekDayDetailResponseList(List.of(session1));

    assertEquals(7, res.size());

    LocalDate date2025_12_22 = LocalDate.of(2025, 12, 22);
    LocalDate date2025_12_23 = LocalDate.of(2025, 12, 23);
    LocalDate date2025_12_24 = LocalDate.of(2025, 12, 24);
    LocalDate date2025_12_25 = LocalDate.of(2025, 12, 25);
    LocalDate date2025_12_26 = LocalDate.of(2025, 12, 26);
    LocalDate date2025_12_27 = LocalDate.of(2025, 12, 27);
    LocalDate date2025_12_28 = LocalDate.of(2025, 12, 28);

    assertAll(
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_22))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_23))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_24))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_25))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_26))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_27))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_28))),
        () ->
            assertEquals(
                1,
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_24))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .size()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_23))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .isEmpty()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_22))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .isEmpty()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_26))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .isEmpty()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_27))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .isEmpty()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_28))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .isEmpty()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_25))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .getFirst()
                    .endTime()
                    .equals(LocalDateTime.of(2025, 12, 25, 0, 18))));
  }

  @Test
  public void toLastWeekDayDetailResponseList_withBleedingSessions_returnsCorrectList() {
    List<LastWeekDayDetailResponse> res =
        this.mapper.toLastWeekDayDetailResponseList(List.of(session1, session2, session4));

    assertEquals(7, res.size());

    LocalDate date2025_12_22 = LocalDate.of(2025, 12, 22);
    LocalDate date2025_12_23 = LocalDate.of(2025, 12, 23);
    LocalDate date2025_12_24 = LocalDate.of(2025, 12, 24);
    LocalDate date2025_12_25 = LocalDate.of(2025, 12, 25);
    LocalDate date2025_12_26 = LocalDate.of(2025, 12, 26);
    LocalDate date2025_12_27 = LocalDate.of(2025, 12, 27);
    LocalDate date2025_12_28 = LocalDate.of(2025, 12, 28);
    LocalDate date2025_12_31 = LocalDate.of(2025, 12, 31);

    assertAll(
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_22))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_23))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_24))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_25))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_26))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_27))),
        () -> assertTrue(res.stream().anyMatch(i -> i.date().equals(date2025_12_28))),
        () -> assertTrue(res.stream().noneMatch(i -> i.date().equals(date2025_12_31))),
        () ->
            assertEquals(
                2,
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_27))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .size()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_22))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .isEmpty()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_23))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .isEmpty()),
        () ->
            assertTrue(
                res.stream()
                    .filter(i -> i.date().equals(date2025_12_26))
                    .findFirst()
                    .orElseThrow()
                    .sessions()
                    .isEmpty()));
  }

  // toSessionIntervalDtoMap
  @Test
  public void toSessionIntervalDtoMap_withNoSession_returnsEmptyMap() {

    Map<LocalDate, List<SessionIntervalDto>> res = this.mapper.toSessionIntervalDtoMap(List.of());

    assertTrue(res.isEmpty());
  }

  @Test
  public void toSessionIntervalDtoMap_withEdgeDayOverlappingSessions_returnsCorrectMap() {

    Map<LocalDate, List<SessionIntervalDto>> res =
        this.mapper.toSessionIntervalDtoMap(List.of(session1, session2, session3, session4));

    assertEquals(6, res.size());

    LocalDate date2025_12_24 = LocalDate.of(2025, 12, 24);
    LocalDate date2025_12_25 = LocalDate.of(2025, 12, 25);
    LocalDate date2025_12_27 = LocalDate.of(2025, 12, 27);
    LocalDate date2025_12_28 = LocalDate.of(2025, 12, 28);
    LocalDate date2025_12_31 = LocalDate.of(2025, 12, 31);
    LocalDate date2026_1_1 = LocalDate.of(2026, 1, 1);

    assertAll(
        () -> assertTrue(res.containsKey(LocalDate.of(2025, 12, 24))),
        () -> assertTrue(res.containsKey(LocalDate.of(2025, 12, 25))),
        () -> assertTrue(res.containsKey(LocalDate.of(2025, 12, 27))),
        () -> assertTrue(res.containsKey(LocalDate.of(2025, 12, 28))),
        () -> assertTrue(res.containsKey(LocalDate.of(2025, 12, 31))),
        () -> assertTrue(res.containsKey(LocalDate.of(2026, 1, 1))));

    assertAll(
        () -> assertEquals(1, res.get(date2025_12_24).size()),
        () -> assertEquals(1, res.get(date2025_12_25).size()),
        () -> assertEquals(2, res.get(date2025_12_27).size()),
        () -> assertEquals(1, res.get(date2025_12_28).size()),
        () -> assertEquals(1, res.get(date2025_12_31).size()),
        () -> assertEquals(1, res.get(date2026_1_1).size()));

    assertAll(
        () ->
            assertEquals(
                LocalDateTime.of(date2025_12_25, LocalTime.of(0, 0)),
                res.get(date2025_12_24).getFirst().endTime()),
        () ->
            assertEquals(
                LocalDateTime.of(date2025_12_25, LocalTime.of(0, 0)),
                res.get(date2025_12_25).getFirst().startTime()),
        () ->
            assertEquals(
                LocalDateTime.of(date2025_12_25, LocalTime.of(0, 18)),
                res.get(date2025_12_25).getFirst().endTime()),
        () ->
            assertEquals(
                LocalDateTime.of(date2026_1_1, LocalTime.of(0, 0)),
                res.get(date2025_12_31).getFirst().endTime()),
        () ->
            assertEquals(
                LocalDateTime.of(date2026_1_1, LocalTime.of(0, 25)),
                res.get(date2026_1_1).getFirst().endTime()));
  }

  // splitSessionAtMidnight
  @Test
  public void
      splitSessionAtMidnight_withSessionWhichSpansOverMidnight_correctlyReturnsTwoSessions() {
    LocalDateTime startTime = LocalDateTime.of(2026, 5, 1, 23, 43);
    LocalDateTime endTime = LocalDateTime.of(2026, 5, 2, 0, 17);
    SessionIntervalDto testSession = new SessionIntervalDto(startTime, endTime);
    List<SessionIntervalDto> res = this.mapper.splitSessionAtMidnight(testSession).toList();

    assertAll(
        () -> assertEquals(2, res.size()),
        () ->
            assertTrue(
                res.stream()
                    .anyMatch(
                        s ->
                            s.startTime().equals(startTime)
                                && s.endTime().equals(LocalDateTime.of(2026, 5, 2, 0, 0)))),
        () ->
            assertTrue(
                res.stream()
                    .anyMatch(
                        s ->
                            s.startTime().equals(LocalDateTime.of(2026, 5, 2, 0, 0))
                                && s.endTime().equals(endTime))));
  }

  @Test
  public void splitSessionAtMidnight_withSameDaySession_correctlyReturnsOneSession() {
    LocalDateTime startTime = LocalDateTime.of(2026, 5, 1, 12, 13);
    LocalDateTime endTime = LocalDateTime.of(2026, 5, 1, 14, 15);
    SessionIntervalDto testSession = new SessionIntervalDto(startTime, endTime);

    List<SessionIntervalDto> res = this.mapper.splitSessionAtMidnight(testSession).toList();

    assertAll(
        () -> assertEquals(1, res.size()),
        () -> assertTrue(res.getFirst().startTime().equals(startTime)),
        () -> assertTrue(res.getFirst().endTime().equals(endTime)));
  }

  // ********************************************************************************
  // AverageWeekResponse
  // ********************************************************************************

  // ********************************************************************************
  // AverageWeekDetailResponse
  // ********************************************************************************

  // calculateContributingDays
  @Test
  public void calculateContributingDays_CalledWithMaxTimeframe_correctlyReturnsArrayFilledWith8() {
    int[] res = this.mapper.calculateContributingDays(56);

    assertAll(
        () -> assertEquals(7, res.length),
        () -> assertTrue(Arrays.stream(res).allMatch(i -> i == 8)));
  }

  @Test
  public void calculateContributingDays_CalledWith10days_correctlyReturns() {
    int[] res = this.mapper.calculateContributingDays(10);

    // 1.1.2026 was a thursday so we expect mo - mi to have 2, the rest 1
    assertAll(
        () -> assertEquals(7, res.length),
        () -> assertEquals(2, res[0]),
        () -> assertEquals(2, res[1]),
        () -> assertEquals(2, res[2]),
        () -> assertEquals(1, res[3]),
        () -> assertEquals(1, res[4]),
        () -> assertEquals(1, res[5]),
        () -> assertEquals(1, res[6]));
  }

  @Test
  public void calculateContributingDays_CalledWith0days_correctlyReturnsArrayFilledWith0() {
    int[] res = this.mapper.calculateContributingDays(0);

    assertAll(
        () -> assertEquals(7, res.length),
        () -> assertTrue(Arrays.stream(res).allMatch(i -> i == 0)));
  }
}

package at.ac.tuwien.inso.ctrl.unitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.session.dto.AverageDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.AverageDayResponse;
import at.ac.tuwien.inso.ctrl.session.dto.LastWeekDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.LastWeekDayResponse;
import at.ac.tuwien.inso.ctrl.session.entity.DailySummary;
import at.ac.tuwien.inso.ctrl.session.entity.DayOfWeekAverage;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.mapper.AnalyticsMapper;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.session.services.AnalyticsService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

  @Mock private Clock clock;
  @Mock private SessionRepository sessionRepository;
  @Mock private AnalyticsMapper analyticsMapper;

  @InjectMocks private AnalyticsService analyticsService;

  private static final Clock FIXED_CLOCK =
      Clock.fixed(LocalDateTime.of(2026, 5, 1, 0, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    analyticsService = new AnalyticsService(FIXED_CLOCK, sessionRepository, analyticsMapper);
    ReflectionTestUtils.setField(analyticsService, "avgTimeframeInWeeks", 8);
  }

  private User testUser() {
    return User.builder()
        .id(UUID.randomUUID())
        .username("testuser")
        .password("password")
        .email("test@example.com")
        .name("Test User")
        .build();
  }

  private SessionHistory session(LocalDateTime start, int minutes) {
    SessionHistory s = new SessionHistory();
    s.setStart(start);
    s.setMinutes((long) minutes);
    return s;
  }

  @Nested
  class GetLastWeek {

    @Test
    void shouldCallRepositoryWithCorrectUserId() {
      User user = testUser();
      when(sessionRepository.findLastWeekSummary(eq(user.getId()), any(), any()))
          .thenReturn(List.of());
      when(analyticsMapper.toLastWeekDayList(any())).thenReturn(List.of());

      analyticsService.getLastWeek(user);

      ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
      ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
      verify(sessionRepository)
          .findLastWeekSummary(eq(user.getId()), fromCaptor.capture(), toCaptor.capture());
      assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.of(2026, 4, 24));
      assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void shouldPassRepositoryResultToMapper() {
      User user = testUser();
      List<DailySummary> summaries = List.of();
      when(sessionRepository.findLastWeekSummary(eq(user.getId()), any(), any()))
          .thenReturn(summaries);
      when(analyticsMapper.toLastWeekDayList(summaries)).thenReturn(List.of());

      analyticsService.getLastWeek(user);

      verify(analyticsMapper).toLastWeekDayList(summaries);
    }

    @Test
    void shouldReturnMapperOutput() {
      User user = testUser();
      List<LastWeekDayResponse> mapperOutput =
          List.of(new LastWeekDayResponse(LocalDate.of(2026, 4, 24), 0L, 0L));
      when(sessionRepository.findLastWeekSummary(eq(user.getId()), any(), any()))
          .thenReturn(List.of());
      when(analyticsMapper.toLastWeekDayList(any())).thenReturn(mapperOutput);

      List<LastWeekDayResponse> result = analyticsService.getLastWeek(user);

      assertThat(result).isEqualTo(mapperOutput);
    }
  }

  @Nested
  class GetLastWeekDetails {

    @Test
    void shouldCallRepositoryWithCorrectUser() {
      User user = testUser();
      when(sessionRepository.findSessionsForLastWeek(eq(user), any(), any())).thenReturn(List.of());
      when(analyticsMapper.toLastWeekDayDetailResponseList(any())).thenReturn(List.of());

      analyticsService.getLastWeekDetails(user);

      verify(sessionRepository).findSessionsForLastWeek(eq(user), any(), any());
    }

    @Test
    void shouldCallRepositoryWithCorrectDateRange() {
      User user = testUser();
      when(sessionRepository.findSessionsForLastWeek(any(), any(), any())).thenReturn(List.of());
      when(analyticsMapper.toLastWeekDayDetailResponseList(any())).thenReturn(List.of());

      analyticsService.getLastWeekDetails(user);

      ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      verify(sessionRepository)
          .findSessionsForLastWeek(eq(user), startCaptor.capture(), endCaptor.capture());
      assertThat(startCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 4, 24, 0, 0));
      assertThat(endCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
    }

    @Test
    void shouldPassRepositoryResultToMapper() {
      User user = testUser();
      List<SessionHistory> sessions = List.of(session(LocalDateTime.of(2026, 4, 27, 10, 0), 30));
      when(sessionRepository.findSessionsForLastWeek(any(), any(), any())).thenReturn(sessions);
      when(analyticsMapper.toLastWeekDayDetailResponseList(sessions)).thenReturn(List.of());

      analyticsService.getLastWeekDetails(user);

      verify(analyticsMapper).toLastWeekDayDetailResponseList(sessions);
    }

    @Test
    void shouldReturnMapperOutput() {
      User user = testUser();
      List<LastWeekDayDetailResponse> mapperOutput = List.of();
      when(sessionRepository.findSessionsForLastWeek(any(), any(), any())).thenReturn(List.of());
      when(analyticsMapper.toLastWeekDayDetailResponseList(any())).thenReturn(mapperOutput);

      List<LastWeekDayDetailResponse> result = analyticsService.getLastWeekDetails(user);

      assertThat(result).isEqualTo(mapperOutput);
    }
  }

  @Nested
  class GetAverageWeek {

    @Test
    void shouldCallRepositoryWithCorrectUserId() {
      User user = testUser();
      user.setCreatedAt(LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
      when(sessionRepository.findDayOfWeekAverages(eq(user.getId()), any(), any()))
          .thenReturn(List.of());
      when(analyticsMapper.toAverageDayResponseList(any())).thenReturn(List.of());

      analyticsService.getAverageWeek(user);

      ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
      ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
      verify(sessionRepository)
          .findDayOfWeekAverages(eq(user.getId()), fromCaptor.capture(), toCaptor.capture());
      assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.of(2026, 3, 6));
      assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void shouldCallRepositoryWithCorrectDaysParameter() {
      User user = testUser();
      user.setCreatedAt(LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
      when(sessionRepository.findDayOfWeekAverages(eq(user.getId()), any(), any()))
          .thenReturn(List.of());
      when(analyticsMapper.toAverageDayResponseList(any())).thenReturn(List.of());

      analyticsService.getAverageWeek(user);

      ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
      ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
      verify(sessionRepository)
          .findDayOfWeekAverages(eq(user.getId()), fromCaptor.capture(), toCaptor.capture());
      assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.of(2026, 3, 6));
      assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void shouldPassRepositoryResultToMapper() {
      User user = testUser();
      user.setCreatedAt(LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
      List<DayOfWeekAverage> averages = List.of();
      when(sessionRepository.findDayOfWeekAverages(eq(user.getId()), any(), any()))
          .thenReturn(averages);
      when(analyticsMapper.toAverageDayResponseList(averages)).thenReturn(List.of());

      analyticsService.getAverageWeek(user);

      verify(analyticsMapper).toAverageDayResponseList(averages);
    }

    @Test
    void shouldReturnMapperOutput() {
      User user = testUser();
      user.setCreatedAt(LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
      List<AverageDayResponse> mapperOutput = List.of(new AverageDayResponse(0, 0.0, 0.0, 0));
      when(sessionRepository.findDayOfWeekAverages(eq(user.getId()), any(), any()))
          .thenReturn(List.of());
      when(analyticsMapper.toAverageDayResponseList(any())).thenReturn(mapperOutput);

      List<AverageDayResponse> result = analyticsService.getAverageWeek(user);

      assertThat(result).isEqualTo(mapperOutput);
    }
  }

  @Nested
  class GetAverageWeekDetails {

    @Test
    void shouldUseFullTimeframeWhenAccountIsOlderThanTimeframe() {
      User user = testUser();
      user.setCreatedAt(LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
      when(sessionRepository.findSessionsForPeriod(eq(user.getId()), any(), any()))
          .thenReturn(List.of());
      when(analyticsMapper.toAverageDayDetailResponseList(any(), anyLong())).thenReturn(List.of());

      analyticsService.getAverageWeekDetails(user);

      ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      verify(sessionRepository)
          .findSessionsForPeriod(eq(user.getId()), fromCaptor.capture(), toCaptor.capture());
      assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 3, 6, 0, 0));
      assertThat(toCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
    }

    @Test
    void shouldCapTimeframeToAccountAgeWhenAccountIsYoungerThanTimeframe() {
      User user = testUser();
      user.setCreatedAt(LocalDateTime.of(2026, 4, 16, 0, 0).toInstant(ZoneOffset.UTC));
      when(sessionRepository.findSessionsForPeriod(eq(user.getId()), any(), any()))
          .thenReturn(List.of());
      when(analyticsMapper.toAverageDayDetailResponseList(any(), anyLong())).thenReturn(List.of());

      analyticsService.getAverageWeekDetails(user);

      ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      verify(sessionRepository)
          .findSessionsForPeriod(eq(user.getId()), fromCaptor.capture(), toCaptor.capture());
      assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 4, 16, 0, 0));
      assertThat(toCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
    }

    @Test
    void shouldPassCorrectTimeframeToBothRepositoryAndMapper() {
      User user = testUser();
      user.setCreatedAt(LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
      List<SessionHistory> sessions = List.of(session(LocalDateTime.of(2026, 4, 27, 10, 0), 30));
      when(sessionRepository.findSessionsForPeriod(eq(user.getId()), any(), any()))
          .thenReturn(sessions);
      when(analyticsMapper.toAverageDayDetailResponseList(sessions, 56L)).thenReturn(List.of());

      analyticsService.getAverageWeekDetails(user);

      ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
      verify(sessionRepository)
          .findSessionsForPeriod(eq(user.getId()), fromCaptor.capture(), toCaptor.capture());
      assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 3, 6, 0, 0));
      assertThat(toCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
      verify(analyticsMapper).toAverageDayDetailResponseList(sessions, 56L);
    }

    @Test
    void shouldReturnMapperOutput() {
      User user = testUser();
      user.setCreatedAt(LocalDateTime.of(2023, 1, 1, 0, 0).toInstant(ZoneOffset.UTC));
      List<AverageDayDetailResponse> mapperOutput =
          List.of(new AverageDayDetailResponse("", 0, List.of(), 0));
      when(sessionRepository.findSessionsForPeriod(eq(user.getId()), any(), any()))
          .thenReturn(List.of());
      when(analyticsMapper.toAverageDayDetailResponseList(any(), anyLong()))
          .thenReturn(mapperOutput);

      List<AverageDayDetailResponse> result = analyticsService.getAverageWeekDetails(user);

      assertThat(result).isEqualTo(mapperOutput);
    }
  }
}

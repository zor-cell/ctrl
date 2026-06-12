package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.SessionLimitExceededException;
import at.ac.tuwien.inso.ctrl.session.dto.SessionConfigDto;
import at.ac.tuwien.inso.ctrl.session.dto.SessionConfigUpdateResponseDto;
import at.ac.tuwien.inso.ctrl.session.entity.DailySessionStatsProjection;
import at.ac.tuwien.inso.ctrl.session.entity.SessionConfig;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.mapper.SessionMapper;
import at.ac.tuwien.inso.ctrl.session.repository.SessionConfigRepository;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.session.services.SessionService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SessionServiceUnitTest {

  @Mock private SessionRepository sessionRepository;
  @Mock private SessionConfigRepository sessionConfigRepository;
  @Mock private SessionMapper sessionMapper;

  @InjectMocks private SessionService sessionService;

  @Captor private ArgumentCaptor<SessionHistory> sessionHistoryCaptor;
  @Captor private ArgumentCaptor<SessionConfig> sessionConfigCaptor;

  private User testUser;
  private SessionConfig defaultConfig;
  private DailySessionStatsProjection statsMock;

  @BeforeEach
  void setUp() {
    // Set @Value properties that are normally injected by Spring
    ReflectionTestUtils.setField(sessionService, "MAX_TIME_BETWEEN_HEARTBEATS", 3L);
    ReflectionTestUtils.setField(sessionService, "SNOOZE_DURATION_MINUTES", 5);

    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("sessionUser");

    defaultConfig =
        SessionConfig.builder()
            .user(testUser)
            .timeLimitMinutes(30)
            .postLimit(100)
            .allowSnooze(true)
            .build();

    statsMock = mock(DailySessionStatsProjection.class);
  }

  @Test
  void handleHeartbeat_StartsNewSession_WhenLastSessionIsTooOld() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime oldStart = now.minusMinutes(10); // Older than max 3 minutes
    SessionHistory oldSession =
        SessionHistory.builder().start(oldStart).seconds(10L).user(testUser).build();

    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));
    when(sessionRepository.findTopByUserOrderByStartDesc(testUser))
        .thenReturn(Optional.of(oldSession));

    sessionService.handleHeartbeat(now, testUser);

    verify(sessionRepository).save(sessionHistoryCaptor.capture());
    SessionHistory savedSession = sessionHistoryCaptor.getValue();
    assertEquals(now, savedSession.getStart());
    assertEquals(0L, savedSession.getSeconds());
  }

  @Test
  void handleHeartbeat_ContinuesSession_WhenWithinTimeframe() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime recentStart = now.minusMinutes(1); // Within max 3 minutes
    SessionHistory recentSession =
        SessionHistory.builder().start(recentStart).seconds(0L).user(testUser).build();

    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));
    when(sessionRepository.findTopByUserOrderByStartDesc(testUser))
        .thenReturn(Optional.of(recentSession));

    sessionService.handleHeartbeat(now, testUser);

    verify(sessionRepository).save(sessionHistoryCaptor.capture());
    SessionHistory savedSession = sessionHistoryCaptor.getValue();
    assertEquals(recentStart, savedSession.getStart());
    assertTrue(savedSession.getSeconds() > 0); // Duration should be updated
  }

  @Test
  void handleHeartbeat_ResetsSnooze_WhenTimeIsExceededAndSnoozeExpired() {
    LocalDateTime now = LocalDateTime.now();
    defaultConfig.setLastSnooze(now.minusMinutes(10)); // Snooze expired (limit is 5)

    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));
    when(sessionRepository.findTopByUserOrderByStartDesc(testUser)).thenReturn(Optional.empty());

    sessionService.handleHeartbeat(now, testUser);

    verify(sessionConfigRepository).save(sessionConfigCaptor.capture());
    assertNull(sessionConfigCaptor.getValue().getLastSnooze());
  }

  @Test
  void snooze_ShouldUpdateLastSnooze_WhenAllowed() {
    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));

    sessionService.snooze(testUser);

    verify(sessionConfigRepository).save(sessionConfigCaptor.capture());
    assertNotNull(sessionConfigCaptor.getValue().getLastSnooze());
  }

  @Test
  void snooze_ShouldThrowException_WhenNotAllowed() {
    defaultConfig.setAllowSnooze(false);
    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));

    assertThrows(InvalidRequestException.class, () -> sessionService.snooze(testUser));
    verify(sessionConfigRepository, never()).save(any());
  }

  @Test
  void updateConfig_AppliesImmediately_WhenLimitsNotExceeded() {
    SessionConfigDto request = new SessionConfigDto(45, 150, false);
    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));
    when(sessionRepository.getDailyStats(eq(testUser), any(LocalDateTime.class)))
        .thenReturn(statsMock);
    when(statsMock.getTotalSeconds()).thenReturn(100L); // well under 30min (1800s) limit
    when(sessionConfigRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    SessionConfigUpdateResponseDto response = sessionService.updateConfig(testUser, request);

    assertTrue(response.appliedImmediately());
    assertEquals(45, response.timeLimitMinutes());
    assertEquals(150, response.postLimit());

    verify(sessionConfigRepository).save(sessionConfigCaptor.capture());
    SessionConfig savedConfig = sessionConfigCaptor.getValue();
    assertEquals(45, savedConfig.getTimeLimitMinutes());
    assertNull(savedConfig.getNextTimeLimitMinutes());
  }

  @Test
  void updateConfig_AppliesNextDay_WhenLimitsAlreadyExceeded() {
    SessionConfigDto request = new SessionConfigDto(45, 150, false);
    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));
    when(sessionRepository.getDailyStats(eq(testUser), any(LocalDateTime.class)))
        .thenReturn(statsMock);
    when(statsMock.getTotalSeconds()).thenReturn(2000L); // 2000s > 1800s (30min limit)
    when(sessionConfigRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    SessionConfigUpdateResponseDto response = sessionService.updateConfig(testUser, request);

    assertFalse(response.appliedImmediately());

    verify(sessionConfigRepository).save(sessionConfigCaptor.capture());
    SessionConfig savedConfig = sessionConfigCaptor.getValue();
    assertEquals(30, savedConfig.getTimeLimitMinutes());
    assertEquals(45, savedConfig.getNextTimeLimitMinutes());
  }

  @Test
  void verifyLimitsAndThrow_DoesNotThrow_WhenSnoozeIsActive() {
    defaultConfig.setLastSnooze(LocalDateTime.now());
    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));

    assertDoesNotThrow(() -> sessionService.verifyLimitsAndThrow(testUser));
    verify(sessionRepository, never()).getDailyStats(any(), any());
  }

  @Test
  void verifyLimitsAndThrow_ThrowsException_WhenTimeLimitExceeded() {
    defaultConfig.setLastSnooze(null);
    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));
    when(sessionRepository.getDailyStats(eq(testUser), any(LocalDateTime.class)))
        .thenReturn(statsMock);
    when(statsMock.getTotalSeconds()).thenReturn(1801L); // 30 mins * 60 = 1800s

    assertThrows(
        SessionLimitExceededException.class, () -> sessionService.verifyLimitsAndThrow(testUser));
  }

  @Test
  void verifyLimitsAndThrow_ThrowsException_WhenPostLimitExceeded() {
    defaultConfig.setLastSnooze(null);
    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));
    when(sessionRepository.getDailyStats(eq(testUser), any(LocalDateTime.class)))
        .thenReturn(statsMock);
    when(statsMock.getTotalSeconds()).thenReturn(100L);
    when(statsMock.getTotalPostsViewed()).thenReturn(101L); // Limit is 100

    assertThrows(
        SessionLimitExceededException.class, () -> sessionService.verifyLimitsAndThrow(testUser));
  }

  @Test
  void verifyLimitsAndThrow_DoesNotThrow_WhenPostLimitExceededButSnoozeIsActive() {
    defaultConfig.setLastSnooze(LocalDateTime.now());
    when(sessionConfigRepository.findByUserId(testUser.getId()))
        .thenReturn(Optional.of(defaultConfig));

    assertDoesNotThrow(() -> sessionService.verifyLimitsAndThrow(testUser));
  }
}

package at.ac.tuwien.inso.ctrl.session.services;

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
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SessionService {
  private final SessionRepository sessionRepository;
  private final SessionConfigRepository sessionConfigRepository;
  private final SessionMapper sessionMapper;

  @Value("${application.session.max-time-between-heartbeats:3}")
  private long MAX_TIME_BETWEEN_HEARTBEATS;

  @Value("${application.session.snooze-time:5}")
  private int SNOOZE_DURATION_MINUTES = 5;

  @Transactional
  public void handleHeartbeat(LocalDateTime timestamp, User user) {
    SessionConfig config = getOrCreateConfig(user);
    SessionHistory session = getOrCreateLastSession(user);

    // if the last heartbeat is more than 3 minutes ago, start a new Session,
    // otherwise compute and save the new duration
    if (session
        .getStart()
        .plusSeconds(session.getSeconds() + MAX_TIME_BETWEEN_HEARTBEATS * 60)
        .isAfter(LocalDateTime.now())) {
      long duration = Duration.between(session.getStart(), LocalDateTime.now()).toSeconds();
      session.setSeconds(duration);
      sessionRepository.save(session);
    } else {
      SessionHistory newSession =
          SessionHistory.builder().start(timestamp).seconds(0L).user(user).postsViewed(0).build();
      sessionRepository.save(newSession);
    }

    if (config.getLastSnooze() != null && !isSnoozed(config)) {
      // snooze time limit reached, reset the snooze timestamp
      config.setLastSnooze(null);
      sessionConfigRepository.save(config);
    }
  }

  @Transactional
  public void snooze(User currentUser) {
    SessionConfig config = getOrCreateConfig(currentUser);
    if (!config.isAllowSnooze()) {
      throw new InvalidRequestException(
          "Snooze cannot be applied because of session configuration");
    }

    config.setLastSnooze(LocalDateTime.now());
    sessionConfigRepository.save(config);
  }

  @Transactional
  public SessionConfigDto getConfig(User currentUser) {
    SessionConfig config = getOrCreateConfig(currentUser);

    return sessionMapper.toConfig(config);
  }

  @Transactional
  public SessionConfigUpdateResponseDto updateConfig(
      User currentUser, SessionConfigDto sessionConfigDto) {
    SessionConfig config = getOrCreateConfig(currentUser);
    DailySessionStatsProjection stats =
        sessionRepository.getDailyStats(currentUser, LocalDate.now().atStartOfDay());

    boolean isAlreadyExceeded = isExceeded(config, stats);
    if (isAlreadyExceeded) {
      // limits are stored and only take effect on the next day
      config.setNextTimeLimitMinutes(sessionConfigDto.timeLimitMinutes());
      config.setNextPostLimit(sessionConfigDto.postLimit());
      config.setNextAllowSnooze(sessionConfigDto.allowSnooze());
    } else {
      // limits are updated immediately if limit is not reached
      config.setTimeLimitMinutes(sessionConfigDto.timeLimitMinutes());
      config.setPostLimit(sessionConfigDto.postLimit());
      config.setAllowSnooze(sessionConfigDto.allowSnooze());

      config.setNextTimeLimitMinutes(null);
      config.setNextPostLimit(null);
      config.setNextAllowSnooze(null);
    }

    var savedConfig = sessionConfigRepository.save(config);

    return new SessionConfigUpdateResponseDto(
        savedConfig.getTimeLimitMinutes(),
        savedConfig.getPostLimit(),
        sessionConfigDto.allowSnooze(),
        !isAlreadyExceeded);
  }

  public SessionHistory getOrCreateLastSession(User user) {
    Optional<SessionHistory> latest = sessionRepository.findTopByUserOrderByStartDesc(user);
    if (latest.isEmpty()) {
      SessionHistory newSession =
          SessionHistory.builder()
              .start(LocalDateTime.now())
              .seconds(0L)
              .user(user)
              .postsViewed(0)
              .build();
      sessionRepository.save(newSession);
      return newSession;
    }

    return latest.get();
  }

  public SessionConfig getOrCreateConfig(User user) {
    return sessionConfigRepository
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              SessionConfig defaultConfig =
                  SessionConfig.builder()
                      .user(user)
                      .timeLimitMinutes(30)
                      .postLimit(100)
                      .allowSnooze(false)
                      .build();

              return sessionConfigRepository.save(defaultConfig);
            });
  }

  public void verifyLimitsAndThrow(User user) {
    SessionConfig config = getOrCreateConfig(user);

    if (isSnoozed(config)) {
      return;
    }

    DailySessionStatsProjection stats =
        sessionRepository.getDailyStats(user, LocalDate.now().atStartOfDay());
    if (stats.getTotalSeconds() >= config.getTimeLimitMinutes() * 60L) {
      throw new SessionLimitExceededException(
          "Your configured daily time limit of %s minute%s was exceeded"
              .formatted(
                  config.getTimeLimitMinutes(), config.getTimeLimitMinutes() == 1 ? "" : "s"));
    }

    if (stats.getTotalPostsViewed() >= config.getPostLimit()) {
      throw new SessionLimitExceededException(
          "The configured daily post limit of %s post%s was exceeded"
              .formatted(config.getPostLimit(), config.getPostLimit() == 1 ? "" : "s"));
    }
  }

  private boolean isSnoozed(SessionConfig config) {
    return config.isAllowSnooze()
        && config.getLastSnooze() != null
        && config.getLastSnooze().plusMinutes(SNOOZE_DURATION_MINUTES).isAfter(LocalDateTime.now());
  }

  private boolean isExceeded(SessionConfig config, DailySessionStatsProjection stats) {
    return stats.getTotalSeconds() >= config.getTimeLimitMinutes() * 60L
        || stats.getTotalPostsViewed() >= config.getPostLimit();
  }
}

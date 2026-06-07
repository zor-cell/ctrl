package at.ac.tuwien.inso.ctrl.session.services;

import at.ac.tuwien.inso.ctrl.exception.SessionLimitExceededException;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.session.dto.SessionConfigDto;
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

  @Value("${application.heartbeat.max-time-between-heartbeats:3}")
  private long MAX_TIME_BETWEEN_HEARTBEATS;

  @Transactional
  public boolean handleHeartbeat(LocalDateTime timestamp, User user) {
    SessionConfig config = getOrCreateConfig(user);
    LocalDateTime trackingStart = getTrackingStart(config);

    DailySessionStatsProjection currentStats = sessionRepository.getDailyStats(user, trackingStart);
    // drop the heartbeat if session limit is already reached
    if (currentStats.getTotalMinutes() >= config.getTimeLimitMinutes()
        || currentStats.getTotalPostsViewed() >= config.getPostLimit()) {
      return false;
    }

    SessionHistory session = getOrCreateLastSession(user);

    // if the last heartbeat is more than 3 minutes ago, start a new Session,
    // otherwise compute and save the new duration
    if (session
        .getStart()
        .plusMinutes(session.getMinutes() + MAX_TIME_BETWEEN_HEARTBEATS)
        .isAfter(LocalDateTime.now())) {
      long duration = Duration.between(session.getStart(), LocalDateTime.now()).toMinutes();
      session.setMinutes(duration);
      sessionRepository.save(session);
    } else {
      SessionHistory newSession =
          SessionHistory.builder().start(timestamp).minutes(0L).user(user).postsViewed(0).build();
      sessionRepository.save(newSession);
    }

    // check if this heartbeat crossed the limit threshold
    DailySessionStatsProjection updatedStats = sessionRepository.getDailyStats(user, trackingStart);
    return updatedStats.getTotalMinutes() >= config.getTimeLimitMinutes()
        || updatedStats.getTotalPostsViewed() >= config.getPostLimit();
  }

  @Transactional
  public void resetSoftCutoff(User currentUser) {
    LocalDateTime exactResetTime = LocalDateTime.now();

    SessionConfig config = getOrCreateConfig(currentUser);
    config.setLastSoftCutoff(exactResetTime);

    sessionConfigRepository.save(config);

    // split the current session to start fresh for this soft cutoff
    SessionHistory currentSession = getOrCreateLastSession(currentUser);
    if (currentSession.getStart().isBefore(exactResetTime)) {
      // save duration for previous session
      long duration = Duration.between(currentSession.getStart(), exactResetTime).toMinutes();
      currentSession.setMinutes(duration);
      sessionRepository.save(currentSession);

      // create a new session
      SessionHistory newSession =
          SessionHistory.builder()
              .start(exactResetTime)
              .minutes(0L)
              .user(currentUser)
              .postsViewed(0)
              .build();

      sessionRepository.save(newSession);
    }
  }

  @Transactional
  public SessionConfigDto getConfig(User currentUser) {
    SessionConfig config = getOrCreateConfig(currentUser);

    return sessionMapper.toConfig(config);
  }

  @Transactional
  public SessionConfigDto updateConfig(User currentUser, SessionConfigDto sessionConfigDto) {
    SessionConfig config = getOrCreateConfig(currentUser);

    //limits are stored and only take effect on the next day
    config.setNextTimeLimitMinutes(sessionConfigDto.timeLimitMinutes());
    config.setNextPostLimit(sessionConfigDto.postLimit());
    config.setNextIsForcedCutoff(sessionConfigDto.isForcedCutoff());

    return sessionMapper.toConfig(sessionConfigRepository.save(config));
  }

  public SessionHistory getOrCreateLastSession(User user) {
    Optional<SessionHistory> latest = sessionRepository.findTopByUserOrderByStartDesc(user);
    if (latest.isEmpty()) {
      SessionHistory newSession =
          SessionHistory.builder()
              .start(LocalDateTime.now())
              .minutes(0L)
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
                      .isForcedCutoff(true)
                      .build();

              return sessionConfigRepository.save(defaultConfig);
            });
  }

  public LocalDateTime getTrackingStart(SessionConfig config) {
    LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
    if (config.getLastSoftCutoff() != null && config.getLastSoftCutoff().isAfter(startOfToday)) {
      return config.getLastSoftCutoff();
    }

    return startOfToday;
  }

  public void verifyLimitsAndThrow(User user) {
    SessionConfig config = getOrCreateConfig(user);
    LocalDateTime trackingStart = getTrackingStart(config);

    DailySessionStatsProjection stats = sessionRepository.getDailyStats(user, trackingStart);
    if (stats.getTotalMinutes() >= config.getTimeLimitMinutes()) {
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
}

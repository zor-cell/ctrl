package at.ac.tuwien.inso.ctrl.session.services;

import at.ac.tuwien.inso.ctrl.algorithm.dto.AlgorithmConfigDto;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.embedding.VectorUtils;
import at.ac.tuwien.inso.ctrl.session.SessionMapper;
import at.ac.tuwien.inso.ctrl.session.dto.SessionConfigDto;
import at.ac.tuwien.inso.ctrl.session.entity.SessionConfig;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.repository.SessionConfigRepository;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
  public void handleHeatbeat(LocalDateTime timestamp, User user) {
    Optional<SessionHistory> latest = sessionRepository.findTopByUserOrderByStartDesc(user);
    if (latest.isEmpty()) {
      SessionHistory newSession =
          SessionHistory.builder().start(timestamp).minutes(0L).user(user).postsViewed(0).build();
      sessionRepository.save(newSession);
      return;
    }

    SessionHistory session = latest.get();
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
  }

  @Transactional
  public SessionConfigDto getConfig(User currentUser) {
    SessionConfig config = getOrCreateConfig(currentUser);

    return sessionMapper.toConfig(config);
  }

  @Transactional
  public SessionConfigDto updateConfig(User currentUser, SessionConfigDto sessionConfigDto) {
    SessionConfig config = getOrCreateConfig(currentUser);

    config.setTimeLimitMinutes(sessionConfigDto.timeLimitMinutes());
    config.setPostLimit(sessionConfigDto.postLimit());
    config.setForcedCutoff(sessionConfigDto.isForcedCutoff());

    return sessionMapper.toConfig(sessionConfigRepository.save(config));
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
}

package at.ac.tuwien.inso.ctrl.session.services;

import at.ac.tuwien.inso.ctrl.exception.SessionLimitExceededException;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.session.annotations.CheckSessionLimits;
import at.ac.tuwien.inso.ctrl.session.entity.DailySessionStatsProjection;
import at.ac.tuwien.inso.ctrl.session.entity.SessionConfig;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class SessionLimitInterceptor implements HandlerInterceptor {
  private final SessionService sessionService;
  private final SessionRepository sessionRepository;
  private final SeenPostRepository seenPostRepository;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // only intercept an actual controller method
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    boolean hasAnnotation =
        handlerMethod.hasMethodAnnotation(CheckSessionLimits.class)
            || handlerMethod.getBeanType().isAnnotationPresent(CheckSessionLimits.class);
    if (!hasAnnotation) {
      return true;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      return true; // allow public endpoints to pass through
    }

    User user = (User) authentication.getPrincipal();
    SessionConfig config = sessionService.getOrCreateConfig(user);

    LocalDateTime trackingStart = LocalDate.now().atStartOfDay();

    // update the start of the tracking if soft cutoff was used
    if (config.getLastSoftCutoff() != null && config.getLastSoftCutoff().isAfter(trackingStart)) {
      trackingStart = config.getLastSoftCutoff();
    }

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

    return true;
  }
}

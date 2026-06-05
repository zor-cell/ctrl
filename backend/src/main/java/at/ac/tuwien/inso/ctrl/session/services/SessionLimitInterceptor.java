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
    sessionService.verifyLimitsAndThrow(user);

    return true;
  }
}

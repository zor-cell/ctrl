package at.ac.tuwien.inso.ctrl.config;

import at.ac.tuwien.inso.ctrl.session.services.SessionLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
  private final SessionLimitInterceptor sessionLimitInterceptor;

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    // append /api prefix to rest controllers
    configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // Register the session limit interceptor for all API endpoints
    registry.addInterceptor(sessionLimitInterceptor).addPathPatterns("/api/**");
  }
}

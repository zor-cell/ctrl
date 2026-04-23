package at.ac.tuwien.inso.ctrl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafEmailConfig {

  @Bean
  public ClassLoaderTemplateResolver emailTemplateResolver() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);
    return resolver;
  }

  @Bean
  public TemplateEngine templateEngine(ClassLoaderTemplateResolver emailTemplateResolver) {
    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(emailTemplateResolver);
    return engine;
  }
}


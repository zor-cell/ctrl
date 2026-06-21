package at.ac.tuwien.inso.ctrl.config;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Slf4j
@Configuration
public class AngularForwardConfig implements WebMvcConfigurer {

  private static final String INDEX_PATH = "/app/static/index.html";

  @EventListener(ApplicationReadyEvent.class)
  public void verifyIndexHtmlExists() {
    FileSystemResource index = new FileSystemResource(INDEX_PATH);
    if (!index.exists()) {
      log.error(
          "FATAL: {} does not exist! Angular fallback routing will loop infinitely. "
              + "Check Jib extraDirectories configuration.",
          INDEX_PATH);
    } else {
      log.info("Angular index.html found at {}", INDEX_PATH);
    }
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("file:/static/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              private final Resource index = new FileSystemResource(INDEX_PATH);

              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                if (resourcePath.startsWith("api/")
                    || resourcePath.startsWith("ws/")
                    || resourcePath.startsWith("public/")
                    || resourcePath.startsWith("swagger")) {
                  return null;
                }

                // Root path or empty path -> serve index.html directly
                if (resourcePath.isEmpty()
                    || resourcePath.equals(".")
                    || resourcePath.equals("/")) {
                  return index.exists() ? index : null;
                }

                if (resourcePath.equals("index.html") && !index.exists()) {
                  return null;
                }

                Resource requestedResource = location.createRelative(resourcePath);
                return (requestedResource.exists() && requestedResource.isReadable())
                    ? requestedResource
                    : index;
              }
            });
  }
}

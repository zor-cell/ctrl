package at.ac.tuwien.inso.ctrl.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class AngularForwardConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("file:/app/static/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                if (resourcePath.startsWith("api/")
                    || resourcePath.startsWith("ws/")
                    || resourcePath.startsWith("public/")
                    || resourcePath.startsWith("swagger")) {
                  return null; // causes 404 since it was not already matched by any REST Controller
                }

                Resource requestedResource = location.createRelative(resourcePath);

                if (requestedResource.exists() && requestedResource.isReadable()) {
                  return requestedResource;
                }

                // routes to angular
                return new FileSystemResource("/app/static/index.html");
              }
            });
  }
}

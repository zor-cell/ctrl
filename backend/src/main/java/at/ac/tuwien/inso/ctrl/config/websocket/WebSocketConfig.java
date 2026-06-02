package at.ac.tuwien.inso.ctrl.config.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompJwtChannelInterceptor stompJwtChannelInterceptor;
  private final TaskScheduler webSocketMessageBrokerTaskScheduler;

  @Bean
  public static TaskScheduler webSocketMessageBrokerTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("websocket-heartbeat-");
    return scheduler;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry
        .enableSimpleBroker("/topic", "/queue")
        .setTaskScheduler(webSocketMessageBrokerTaskScheduler)
        .setHeartbeatValue(new long[] {5000, 5000});
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns(
            "http://localhost:*", "http://127.0.0.1:*", "https://www.ctrl-ase.dev");

    registry
        .addEndpoint("/ws-sockjs")
        .setAllowedOriginPatterns(
            "http://localhost:*", "http://127.0.0.1:*", "https://www.ctrl-ase.dev")
        .withSockJS()
        .setInterceptors();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(stompJwtChannelInterceptor);
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration
        .setMessageSizeLimit(512 * 1024)
        .setSendBufferSizeLimit(512 * 1024)
        .setSendTimeLimit(60000);
  }
}

package at.ac.tuwien.inso.ctrl.config.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private static final HandshakeHandler NO_COMPRESSION_HANDSHAKE =
      new NoCompressionHandshakeHandler();

  private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue").setHeartbeatValue(new long[] {0, 0});
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setHandshakeHandler(NO_COMPRESSION_HANDSHAKE)
        .setAllowedOriginPatterns(
            "http://localhost:*", "http://127.0.0.1:*", "https://www.ctrl-ase.dev");

    registry
        .addEndpoint("/ws-sockjs")
        .setHandshakeHandler(NO_COMPRESSION_HANDSHAKE)
        .setAllowedOriginPatterns(
            "http://localhost:*", "http://127.0.0.1:*", "https://www.ctrl-ase.dev")
        .withSockJS();
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

package at.ac.tuwien.inso.ctrl.config.websocket;

import at.ac.tuwien.inso.ctrl.chat.repository.ChatMemberRepository;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.service.CustomUserDetailsService;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtChannelInterceptor implements ChannelInterceptor {

  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;
  private final ChatMemberRepository chatMemberRepository;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null) {
      return message;
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      return handleSubscribe(message, accessor);
    }

    if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }

    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.warn("[WebSocket] CONNECT without valid Authorization header");
      return null;
    }

    String jwt = authHeader.substring("Bearer ".length());
    try {
      String username = jwtService.extractUsername(jwt);
      Optional<UUID> userId = jwtService.extractUserId(jwt);
      UserDetails userDetails =
          userId.map(userDetailsService::loadUserById)
              .orElseGet(() -> userDetailsService.loadUserByUsername(username));
      if (jwtService.isTokenValid(jwt, userDetails)) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("[WebSocket] User authenticated: {}", userDetails.getUsername());
      } else {
        log.warn("[WebSocket] Invalid token for user: {}", username);
        return null;
      }
    } catch (Exception e) {
      log.warn("[WebSocket] JWT validation failed: {}", e.getMessage());
      return null;
    }

    return message;
  }

  private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
    UUID chatId = chatIdFromDestination(accessor.getDestination());
    if (chatId == null) {
      return message;
    }

    User user = currentUser(accessor.getUser());
    if (user == null || !chatMemberRepository.existsActiveByChatChatIdAndUser(chatId, user)) {
      String username = accessor.getUser() != null ? accessor.getUser().getName() : "anonymous";
      log.warn("[WebSocket] User '{}' tried to subscribe to chat '{}'", username, chatId);
      return null;
    }

    return message;
  }

  private UUID chatIdFromDestination(String destination) {
    if (destination == null) {
      return null;
    }

    String chatId = null;
    String userQueuePrefix = "/user/queue/chats/";
    String topicPrefix = "/topic/chats/";

    if (destination.startsWith(userQueuePrefix) && destination.endsWith("/messages")) {
      chatId =
          destination.substring(
              userQueuePrefix.length(), destination.length() - "/messages".length());
    } else if (destination.startsWith(topicPrefix)) {
      chatId = destination.substring(topicPrefix.length());
    }

    if (chatId == null || chatId.isBlank()) {
      return null;
    }

    try {
      return UUID.fromString(chatId);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private User currentUser(Principal principal) {
    if (principal instanceof UsernamePasswordAuthenticationToken authentication
        && authentication.getPrincipal() instanceof User user) {
      return user;
    }

    return null;
  }
}

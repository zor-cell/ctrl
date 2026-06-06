package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.chat.repository.ChatMemberRepository;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.config.websocket.StompJwtChannelInterceptor;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.service.CustomUserDetailsService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link StompJwtChannelInterceptor}: CONNECT authentication and SUBSCRIBE
 * authorization for chat destinations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StompJwtChannelInterceptorUnitTest {

  @Mock private JwtService jwtService;
  @Mock private CustomUserDetailsService userDetailsService;
  @Mock private ChatMemberRepository chatMemberRepository;
  @Mock private MessageChannel channel;

  private StompJwtChannelInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor =
        new StompJwtChannelInterceptor(jwtService, userDetailsService, chatMemberRepository);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ---------------------------------------------------------------------------
  // CONNECT
  // ---------------------------------------------------------------------------

  @Test
  void connect_withoutAuthorizationHeader_isRejected() {
    Message<byte[]> message = connectMessage(null);

    assertNull(interceptor.preSend(message, channel));
  }

  @Test
  void connect_withNonBearerHeader_isRejected() {
    Message<byte[]> message = connectMessage("Basic abc");

    assertNull(interceptor.preSend(message, channel));
  }

  @Test
  void connect_withValidToken_isAccepted() {
    User user = createUser("alice");
    when(jwtService.extractUsername("good-token")).thenReturn("alice");
    when(jwtService.extractUserId("good-token")).thenReturn(Optional.of(user.getId()));
    when(userDetailsService.loadUserById(user.getId())).thenReturn(user);
    when(jwtService.isTokenValid(eq("good-token"), any())).thenReturn(true);

    Message<byte[]> message = connectMessage("Bearer good-token");

    assertSame(message, interceptor.preSend(message, channel));
  }

  @Test
  void connect_withInvalidToken_isRejected() {
    User user = createUser("alice");
    when(jwtService.extractUsername("bad-token")).thenReturn("alice");
    when(jwtService.extractUserId("bad-token")).thenReturn(Optional.empty());
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);
    when(jwtService.isTokenValid(eq("bad-token"), any())).thenReturn(false);

    Message<byte[]> message = connectMessage("Bearer bad-token");

    assertNull(interceptor.preSend(message, channel));
  }

  @Test
  void connect_whenValidationThrows_isRejected() {
    when(jwtService.extractUsername(anyString())).thenThrow(new RuntimeException("boom"));

    Message<byte[]> message = connectMessage("Bearer broken-token");

    assertNull(interceptor.preSend(message, channel));
  }

  // ---------------------------------------------------------------------------
  // SUBSCRIBE
  // ---------------------------------------------------------------------------

  @Test
  void subscribe_toChatMessageQueue_asMember_isAccepted() {
    User user = createUser("alice");
    UUID chatId = UUID.randomUUID();
    when(chatMemberRepository.existsActiveByChatChatIdAndUser(chatId, user)).thenReturn(true);

    Message<byte[]> message = subscribeMessage("/user/queue/chats/" + chatId + "/messages", user);

    assertSame(message, interceptor.preSend(message, channel));
  }

  @Test
  void subscribe_toChatMessageQueue_asNonMember_isRejected() {
    User user = createUser("alice");
    UUID chatId = UUID.randomUUID();
    when(chatMemberRepository.existsActiveByChatChatIdAndUser(chatId, user)).thenReturn(false);

    Message<byte[]> message = subscribeMessage("/user/queue/chats/" + chatId + "/messages", user);

    assertNull(interceptor.preSend(message, channel));
  }

  @Test
  void subscribe_toChatTopic_asMember_isAccepted() {
    User user = createUser("alice");
    UUID chatId = UUID.randomUUID();
    when(chatMemberRepository.existsActiveByChatChatIdAndUser(chatId, user)).thenReturn(true);

    Message<byte[]> message = subscribeMessage("/topic/chats/" + chatId, user);

    assertSame(message, interceptor.preSend(message, channel));
  }

  @Test
  void subscribe_toUnrelatedDestination_passesThrough() {
    User user = createUser("alice");

    Message<byte[]> message = subscribeMessage("/user/queue/notifications", user);

    assertNotNull(interceptor.preSend(message, channel));
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private Message<byte[]> connectMessage(String authorizationHeader) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    // Keep the accessor mutable so the interceptor can call accessor.setUser(...) like in
    // production.
    accessor.setLeaveMutable(true);
    if (authorizationHeader != null) {
      accessor.addNativeHeader("Authorization", authorizationHeader);
    }
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> subscribeMessage(String destination, User user) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private User createUser(String username) {
    return User.builder().id(UUID.randomUUID()).username(username).password("pw").build();
  }
}

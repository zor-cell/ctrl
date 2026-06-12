package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.chat.ChatService;
import at.ac.tuwien.inso.ctrl.chat.dto.AddChatMembersRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatMessageResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatRealtimePayload;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatRemovedPayload;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.CreateChatRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.SendMessageRequest;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatMemberRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.MessageRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.ReactionRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.SharedRepository;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
class WebSocketSecurityIntegrationTest {

  private static final String TEST_SCHEMA = "websocket_test";

  @DynamicPropertySource
  static void configureDatabase(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", WebSocketSecurityIntegrationTest::testDatabaseUrl);
    registry.add("spring.jpa.properties.hibernate.default_schema", () -> TEST_SCHEMA);
    registry.add(
        "spring.datasource.hikari.connection-init-sql",
        () ->
            "CREATE SCHEMA IF NOT EXISTS "
                + TEST_SCHEMA
                + "; CREATE EXTENSION IF NOT EXISTS vector");
  }

  private static String testDatabaseUrl() {
    String configuredUrl = System.getenv("SPRING_DATASOURCE_URL");
    String databaseName =
        System.getenv()
            .getOrDefault("POSTGRES_DB", System.getenv().getOrDefault("DB_NAME", "ctrl"));
    String baseUrl =
        configuredUrl == null || configuredUrl.isBlank()
            ? "jdbc:postgresql://localhost:5433/" + databaseName
            : configuredUrl;
    String separator = baseUrl.contains("?") ? "&" : "?";
    return baseUrl + separator + "currentSchema=" + TEST_SCHEMA + ",public";
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;
  @Autowired private ChatService chatService;
  @Autowired private UserRepository userRepository;
  @Autowired private FriendRelationRepository friendRelationRepository;
  @Autowired private ChatRepository chatRepository;
  @Autowired private ChatMemberRepository chatMemberRepository;
  @Autowired private MessageRepository messageRepository;
  @Autowired private ReactionRepository reactionRepository;
  @Autowired private SharedRepository sharedRepository;
  @Autowired private PostRepository postRepository;
  @Autowired private SimpUserRegistry simpUserRegistry;

  @LocalServerPort private int port;

  private User alice;
  private User bob;
  private User chris;

  @Test
  void wsInfo_isPublic() throws Exception {
    mockMvc.perform(get("/ws-sockjs/info").queryParam("t", "1")).andExpect(status().isOk());
  }

  @BeforeEach
  void setUp() {
    cleanDatabase();

    alice = saveUser("alice-ws", "alice-ws@test.com");
    bob = saveUser("bob-ws", "bob-ws@test.com");
    chris = saveUser("chris-ws", "chris-ws@test.com");
    saveFriendRelation(alice, bob);
    saveFriendRelation(bob, chris);
  }

  @AfterEach
  void tearDown() {
    cleanDatabase();
  }

  @Test
  void authenticatedNativeWebSocketConnection_receivesChatMessageEventsAndUserQueueUpdates()
      throws Exception {
    assertChatMessageIsReceived(createNativeStompClient(), "ws://localhost:" + port + "/ws");
  }

  @Test
  void authenticatedSockJsXhrStreamingConnection_receivesChatMessageEventsAndUserQueueUpdates()
      throws Exception {
    assertChatMessageIsReceived(
        createSockJsStompClient(false), "http://localhost:" + port + "/ws-sockjs");
  }

  @Test
  void authenticatedSockJsXhrPollingConnection_receivesChatMessageEventsAndUserQueueUpdates()
      throws Exception {
    assertChatMessageIsReceived(
        createSockJsStompClient(true), "http://localhost:" + port + "/ws-sockjs");
  }

  @Test
  void privateSharedPost_isPersonalizedByViewerFriendship() {
    Post privatePost = savePost("private-shared-post", "Secret shared caption", false, alice);
    ChatResponse chat =
        chatService.createChat(
            bob, new CreateChatRequest("privacy_group", List.of("alice-ws", "chris-ws")));

    chatService.sendMessage(
        alice, chat.chatId(), new SendMessageRequest("private share", privatePost.getPostId()));

    ChatMessageResponse bobMessage = getLastMessage(chatService.getMessages(bob, chat.chatId()));
    assertNotNull(bobMessage.sharedPost());
    assertTrue(bobMessage.sharedPost().canView());
    assertFalse(bobMessage.sharedPost().publicPost());
    assertEquals("alice-ws", bobMessage.sharedPost().profileUsername());
    assertEquals("alice-ws", bobMessage.sharedPost().username());
    assertEquals("Secret shared caption", bobMessage.sharedPost().caption());
    assertNotNull(bobMessage.sharedPost().mediaRef());

    ChatMessageResponse chrisMessage =
        getLastMessage(chatService.getMessages(chris, chat.chatId()));
    assertNotNull(chrisMessage.sharedPost());
    assertFalse(chrisMessage.sharedPost().canView());
    assertFalse(chrisMessage.sharedPost().publicPost());
    assertEquals("alice-ws", chrisMessage.sharedPost().profileUsername());
    assertEquals("alice-ws", chrisMessage.sharedPost().username());
    assertNull(chrisMessage.sharedPost().caption());
    assertNull(chrisMessage.sharedPost().mediaRef());
    assertNull(chrisMessage.sharedPost().mediaKind());
  }

  @Test
  void leavingGroup_removesGroupFromLeavingUsersChatList() {
    ChatResponse chat =
        chatService.createChat(
            bob, new CreateChatRequest("leave_group", List.of("alice-ws", "chris-ws")));

    chatService.leaveChat(alice, chat.chatId());

    assertFalse(
        chatService.getChats(alice).stream()
            .anyMatch(response -> response.chatId().equals(chat.chatId())));
    assertTrue(
        chatService.getChats(bob).stream()
            .anyMatch(response -> response.chatId().equals(chat.chatId())));
    assertTrue(
        chatService.getChats(chris).stream()
            .anyMatch(response -> response.chatId().equals(chat.chatId())));
  }

  @Test
  void createGroup_rejectsMoreThan50Members() {
    List<String> usernames = saveFriendUsernames(alice, "limitcreate", 50);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> chatService.createChat(alice, new CreateChatRequest("too_many", usernames)));

    assertEquals("Group chats can have at most 50 members", exception.getMessage());
  }

  @Test
  void addMembers_rejectsWhenGroupWouldExceed50Members() {
    List<String> initialMembers = saveFriendUsernames(alice, "limitadd", 49);
    ChatResponse chat =
        chatService.createChat(alice, new CreateChatRequest("full_group", initialMembers));
    List<String> extraMembers = saveFriendUsernames(alice, "limitextra", 1);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                chatService.addMembers(
                    alice, chat.chatId(), new AddChatMembersRequest(extraMembers)));

    assertEquals("Group chats can have at most 50 members", exception.getMessage());
  }

  @Test
  void markChatAsRead_clearsUnreadCountWithoutReloadingMessages() {
    ChatResponse chat =
        chatService.createChat(alice, new CreateChatRequest(null, List.of("bob-ws")));
    chatService.sendMessage(alice, chat.chatId(), new SendMessageRequest("unread check", null));

    ChatResponse unreadChat =
        chatService.getChats(bob).stream()
            .filter(response -> response.chatId().equals(chat.chatId()))
            .findFirst()
            .orElseThrow();
    assertEquals(1L, unreadChat.unreadCount());

    chatService.markChatAsRead(bob, chat.chatId());

    ChatResponse readChat =
        chatService.getChats(bob).stream()
            .filter(response -> response.chatId().equals(chat.chatId()))
            .findFirst()
            .orElseThrow();
    assertEquals(0L, readChat.unreadCount());
  }

  @Test
  void privateSharedPost_isNotBroadcastOnSharedTopicAndMessageQueuePayloadIsPersonalized()
      throws Exception {
    Post privatePost = savePost("private-topic-post", "Topic leak check", false, alice);
    ChatResponse chat =
        chatService.createChat(
            bob, new CreateChatRequest("privacy_topic_group", List.of("alice-ws", "chris-ws")));
    BlockingQueue<ChatRealtimePayload> receivedMessages = new ArrayBlockingQueue<>(1);
    BlockingQueue<ChatRealtimePayload> receivedTopicMessages = new ArrayBlockingQueue<>(1);
    BlockingQueue<ChatResponse> receivedSummaries = new ArrayBlockingQueue<>(1);

    WebSocketStompClient stompClient = createNativeStompClient();
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + jwtService.generateToken(chris));

    StompSession session =
        stompClient
            .connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

    try {
      session.subscribe(
          "/topic/chats/" + chat.chatId(),
          new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
              return ChatRealtimePayload.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
              receivedTopicMessages.offer((ChatRealtimePayload) payload);
            }
          });
      waitForSubscription(chris.getUsername(), "/topic/chats/" + chat.chatId());

      session.subscribe(
          "/user/queue/chats/" + chat.chatId() + "/messages",
          new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
              return ChatRealtimePayload.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
              receivedMessages.offer((ChatRealtimePayload) payload);
            }
          });
      waitForSubscription(chris.getUsername(), "/user/queue/chats/" + chat.chatId() + "/messages");

      session.subscribe(
          "/user/queue/chats",
          new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
              return ChatResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
              receivedSummaries.offer((ChatResponse) payload);
            }
          });
      waitForSubscription(chris.getUsername(), "/user/queue/chats");

      chatService.sendMessage(
          alice, chat.chatId(), new SendMessageRequest("hidden live", privatePost.getPostId()));

      assertNull(receivedTopicMessages.poll(750, TimeUnit.MILLISECONDS));
      ChatRealtimePayload messagePayload = receivedMessages.poll(5, TimeUnit.SECONDS);
      assertNotNull(messagePayload);
      assertNotNull(messagePayload.message().sharedPost());
      assertFalse(messagePayload.message().own());
      assertFalse(messagePayload.message().sharedPost().canView());
      assertNull(messagePayload.message().sharedPost().caption());
      assertEquals("alice-ws", messagePayload.message().sharedPost().profileUsername());

      ChatResponse summary = receivedSummaries.poll(5, TimeUnit.SECONDS);
      assertNotNull(summary);
      assertNotNull(summary.lastMessage());
      assertNotNull(summary.lastMessage().sharedPost());
      assertFalse(summary.lastMessage().sharedPost().canView());
      assertNull(summary.lastMessage().sharedPost().caption());
      assertEquals("alice-ws", summary.lastMessage().sharedPost().profileUsername());
    } finally {
      session.disconnect();
      stompClient.stop();
    }
  }

  @Test
  void nonMemberSubscriptionToChatMessageQueue_doesNotReceiveChatMessages() throws Exception {
    ChatResponse chat =
        chatService.createChat(alice, new CreateChatRequest(null, List.of("bob-ws")));
    BlockingQueue<ChatRealtimePayload> receivedMessages = new ArrayBlockingQueue<>(1);

    WebSocketStompClient stompClient = createNativeStompClient();
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + jwtService.generateToken(chris));

    StompSession session =
        stompClient
            .connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

    try {
      session.subscribe(
          "/user/queue/chats/" + chat.chatId() + "/messages",
          new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
              return ChatRealtimePayload.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
              receivedMessages.offer((ChatRealtimePayload) payload);
            }
          });

      chatService.sendMessage(
          alice, chat.chatId(), new SendMessageRequest("private chat event", null));

      assertNull(receivedMessages.poll(750, TimeUnit.MILLISECONDS));
    } finally {
      session.disconnect();
      stompClient.stop();
    }
  }

  @Test
  void groupDeletion_sendsChatRemovedEventToMembers() throws Exception {
    ChatResponse chat =
        chatService.createChat(
            bob, new CreateChatRequest("delete_group", List.of("alice-ws", "chris-ws")));
    BlockingQueue<ChatRemovedPayload> removedEvents = new ArrayBlockingQueue<>(1);

    WebSocketStompClient stompClient = createNativeStompClient();
    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + jwtService.generateToken(chris));

    StompSession session =
        stompClient
            .connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

    try {
      session.subscribe(
          "/user/queue/chats/removed",
          new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
              return ChatRemovedPayload.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
              removedEvents.offer((ChatRemovedPayload) payload);
            }
          });
      waitForSubscription(chris.getUsername(), "/user/queue/chats/removed");

      chatService.deleteChat(bob, chat.chatId());

      ChatRemovedPayload payload = removedEvents.poll(5, TimeUnit.SECONDS);
      assertNotNull(payload);
      assertEquals(chat.chatId(), payload.chatId());
    } finally {
      session.disconnect();
      stompClient.stop();
    }
  }

  private void assertChatMessageIsReceived(WebSocketStompClient stompClient, String url)
      throws Exception {
    ChatResponse chat =
        chatService.createChat(alice, new CreateChatRequest(null, List.of("bob-ws")));
    BlockingQueue<ChatRealtimePayload> receivedMessages = new ArrayBlockingQueue<>(1);
    BlockingQueue<ChatResponse> receivedSummaries = new ArrayBlockingQueue<>(1);

    StompHeaders connectHeaders = new StompHeaders();
    connectHeaders.add("Authorization", "Bearer " + jwtService.generateToken(bob));

    StompSession session =
        stompClient
            .connectAsync(
                url,
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

    session.subscribe(
        "/user/queue/chats/" + chat.chatId() + "/messages",
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return ChatRealtimePayload.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            receivedMessages.offer((ChatRealtimePayload) payload);
          }
        });
    waitForSubscription(bob.getUsername(), "/user/queue/chats/" + chat.chatId() + "/messages");

    session.subscribe(
        "/user/queue/chats",
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return ChatResponse.class;
          }

          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            receivedSummaries.offer((ChatResponse) payload);
          }
        });
    waitForSubscription(bob.getUsername(), "/user/queue/chats");

    chatService.sendMessage(alice, chat.chatId(), new SendMessageRequest("hello over stomp", null));

    ChatRealtimePayload payload = receivedMessages.poll(5, TimeUnit.SECONDS);
    assertNotNull(payload);
    assertEquals("MESSAGE", payload.type());
    assertEquals(chat.chatId(), payload.chatId());
    assertEquals("hello over stomp", payload.message().content());
    assertFalse(payload.message().own());

    ChatResponse summary = receivedSummaries.poll(5, TimeUnit.SECONDS);
    assertNotNull(summary);
    assertEquals(chat.chatId(), summary.chatId());
    assertNotNull(summary.lastMessage());
    assertEquals("hello over stomp", summary.lastMessage().content());
    assertFalse(summary.lastMessage().own());

    session.disconnect();
    stompClient.stop();
  }

  private void waitForSubscription(String username, String destination) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      if (hasSubscription(username, destination)) {
        return;
      }
      Thread.sleep(50);
    }

    fail("Subscription was not registered for user " + username + " at destination " + destination);
  }

  private boolean hasSubscription(String username, String destination) {
    SimpUser user = simpUserRegistry.getUser(username);
    if (user == null) {
      return false;
    }

    return user.getSessions().stream()
        .flatMap(session -> session.getSubscriptions().stream())
        .map(SimpSubscription::getDestination)
        .anyMatch(registeredDestination -> destinationMatches(registeredDestination, destination));
  }

  private boolean destinationMatches(String registeredDestination, String requestedDestination) {
    if (registeredDestination == null) {
      return false;
    }

    if (registeredDestination.equals(requestedDestination)) {
      return true;
    }

    if (requestedDestination.startsWith("/user")) {
      String brokerDestination = requestedDestination.substring("/user".length());
      return registeredDestination.startsWith(brokerDestination);
    }

    return false;
  }

  private WebSocketStompClient createNativeStompClient() {
    return configureStompClient(new WebSocketStompClient(new StandardWebSocketClient()));
  }

  private WebSocketStompClient createSockJsStompClient(boolean xhrStreamingDisabled) {
    RestTemplateXhrTransport xhrTransport = new RestTemplateXhrTransport();
    xhrTransport.setXhrStreamingDisabled(xhrStreamingDisabled);
    SockJsClient sockJsClient = new SockJsClient(List.of(xhrTransport));
    return configureStompClient(new WebSocketStompClient(sockJsClient));
  }

  private WebSocketStompClient configureStompClient(WebSocketStompClient stompClient) {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    converter.setObjectMapper(objectMapper);
    stompClient.setMessageConverter(converter);
    stompClient.setDefaultHeartbeat(new long[] {0, 0});
    return stompClient;
  }

  private User saveUser(String username, String email) {
    return userRepository.save(
        User.builder()
            .username(username)
            .email(email)
            .password("password")
            .name(username)
            .emailVerified(true)
            .isVerified(true)
            .birthdate(LocalDate.of(2000, 1, 1))
            .build());
  }

  private void saveFriendRelation(User firstUser, User secondUser) {
    friendRelationRepository.save(
        FriendRelation.builder()
            .id(new FriendRelationId(firstUser.getId(), secondUser.getId()))
            .friend1(firstUser)
            .friend2(secondUser)
            .since(LocalDateTime.now())
            .build());
  }

  private List<String> saveFriendUsernames(User user, String usernamePrefix, int count) {
    List<String> usernames = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      String username = usernamePrefix + index;
      User friend = saveUser(username, username + "@test.com");
      saveFriendRelation(user, friend);
      usernames.add(username);
    }

    return usernames;
  }

  private Post savePost(String postId, String caption, boolean publicPost, User owner) {
    return postRepository.save(
        Post.builder()
            .postId(postId)
            .user(owner)
            .caption(caption)
            .mediaRef("posts/media/" + postId + ".png")
            .isPublic(publicPost)
            .isAiGenerated(false)
            .isRemoved(false)
            .upvotes(0)
            .downvotes(0)
            .createdAt(LocalDateTime.now())
            .build());
  }

  private ChatMessageResponse getLastMessage(List<ChatMessageResponse> messages) {
    return messages.get(messages.size() - 1);
  }

  private void cleanDatabase() {
    reactionRepository.deleteAll();
    messageRepository.deleteAll();
    sharedRepository.deleteAll();
    chatMemberRepository.deleteAll();
    chatRepository.deleteAll();
    postRepository.deleteAll();
    friendRelationRepository.deleteAll();
    userRepository.deleteAll();
  }
}

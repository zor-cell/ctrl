package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.chat.ChatController;
import at.ac.tuwien.inso.ctrl.chat.ChatService;
import at.ac.tuwien.inso.ctrl.chat.dto.AddChatMembersRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.AddReactionRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatMessageResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.CreateChatRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.SendMessageRequest;
import at.ac.tuwien.inso.ctrl.exception.GlobalExceptionHandler;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/**
 * Standalone MockMvc tests for {@link ChatController}: verify HTTP status codes, request mapping,
 * bean validation and error translation. The {@link ChatService} is mocked, so no database is used.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerIntegrationTest {

  private MockMvc mockMvc;

  @Mock private ChatService chatService;

  private User user;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ChatController(chatService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
    user = createUser("alice");
    // @AuthenticationPrincipal resolves from the SecurityContext, not the request principal.
    SecurityContextHolder.getContext().setAuthentication(authenticationFor(user));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ---------------------------------------------------------------------------
  // read endpoints
  // ---------------------------------------------------------------------------

  @Test
  void getChats_returnsChatList() throws Exception {
    UUID chatId = UUID.randomUUID();
    when(chatService.getChats(any(User.class))).thenReturn(List.of(chatResponse(chatId, "Bob")));

    mockMvc
        .perform(get("/chats").principal(authenticationFor(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].chatId").value(chatId.toString()))
        .andExpect(jsonPath("$[0].name").value("Bob"));

    verify(chatService).getChats(any(User.class));
  }

  @Test
  void getChat_returnsSingleChat() throws Exception {
    UUID chatId = UUID.randomUUID();
    when(chatService.getChat(any(User.class), eq(chatId))).thenReturn(chatResponse(chatId, "Bob"));

    mockMvc
        .perform(get("/chats/" + chatId).principal(authenticationFor(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.chatId").value(chatId.toString()));
  }

  @Test
  void getChat_whenServiceForbids_returnsForbidden() throws Exception {
    UUID chatId = UUID.randomUUID();
    when(chatService.getChat(any(User.class), eq(chatId)))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member"));

    mockMvc
        .perform(get("/chats/" + chatId).principal(authenticationFor(user)))
        .andExpect(status().isForbidden());
  }

  @Test
  void getChat_whenServiceNotFound_returnsNotFound() throws Exception {
    UUID chatId = UUID.randomUUID();
    when(chatService.getChat(any(User.class), eq(chatId)))
        .thenThrow(new NotFoundException("Chat", chatId.toString()));

    mockMvc
        .perform(get("/chats/" + chatId).principal(authenticationFor(user)))
        .andExpect(status().isNotFound());
  }

  // ---------------------------------------------------------------------------
  // createChat
  // ---------------------------------------------------------------------------

  @Test
  void createChat_returnsCreated() throws Exception {
    UUID chatId = UUID.randomUUID();
    when(chatService.createChat(any(User.class), any(CreateChatRequest.class)))
        .thenReturn(chatResponse(chatId, "Bob"));

    mockMvc
        .perform(
            post("/chats")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": null, \"usernames\": [\"bob\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.chatId").value(chatId.toString()));

    verify(chatService).createChat(any(User.class), any(CreateChatRequest.class));
  }

  @Test
  void createChat_withInvalidGroupName_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/chats")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"invalid name!\", \"usernames\": [\"bob\"]}"))
        .andExpect(status().isBadRequest());

    verify(chatService, never()).createChat(any(), any());
  }

  @Test
  void createChat_withEmptyUsernames_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/chats")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": null, \"usernames\": []}"))
        .andExpect(status().isBadRequest());

    verify(chatService, never()).createChat(any(), any());
  }

  // ---------------------------------------------------------------------------
  // messages
  // ---------------------------------------------------------------------------

  @Test
  void sendMessage_returnsCreated() throws Exception {
    UUID chatId = UUID.randomUUID();
    when(chatService.sendMessage(any(User.class), eq(chatId), any(SendMessageRequest.class)))
        .thenReturn(messageResponse(chatId, "hello"));

    mockMvc
        .perform(
            post("/chats/" + chatId + "/messages")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"hello\", \"postIdToShare\": null}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("hello"));
  }

  @Test
  void sendMessage_withTooLongContent_returnsBadRequest() throws Exception {
    UUID chatId = UUID.randomUUID();
    String tooLong = "a".repeat(4001);

    mockMvc
        .perform(
            post("/chats/" + chatId + "/messages")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"" + tooLong + "\", \"postIdToShare\": null}"))
        .andExpect(status().isBadRequest());

    verify(chatService, never()).sendMessage(any(), any(), any());
  }

  @Test
  void sendMessage_whenServiceRejects_returnsBadRequest() throws Exception {
    UUID chatId = UUID.randomUUID();
    when(chatService.sendMessage(any(User.class), eq(chatId), any(SendMessageRequest.class)))
        .thenThrow(new InvalidRequestException("Message content or shared post must be present"));

    mockMvc
        .perform(
            post("/chats/" + chatId + "/messages")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"x\", \"postIdToShare\": null}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void markChatAsRead_returnsNoContent() throws Exception {
    UUID chatId = UUID.randomUUID();

    mockMvc
        .perform(post("/chats/" + chatId + "/read").principal(authenticationFor(user)))
        .andExpect(status().isNoContent());

    verify(chatService).markChatAsRead(any(User.class), eq(chatId));
  }

  // ---------------------------------------------------------------------------
  // reactions
  // ---------------------------------------------------------------------------

  @Test
  void reactToMessage_returnsOk() throws Exception {
    UUID chatId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();
    when(chatService.reactToMessage(
            any(User.class), eq(chatId), eq(messageId), any(AddReactionRequest.class)))
        .thenReturn(messageResponse(chatId, "hello"));

    mockMvc
        .perform(
            post("/chats/" + chatId + "/messages/" + messageId + "/reactions")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emoji\": \"\uD83D\uDC4D\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void reactToMessage_withBlankEmoji_returnsBadRequest() throws Exception {
    UUID chatId = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/chats/" + chatId + "/messages/" + messageId + "/reactions")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emoji\": \"\"}"))
        .andExpect(status().isBadRequest());

    verify(chatService, never()).reactToMessage(any(), any(), any(), any());
  }

  // ---------------------------------------------------------------------------
  // membership / lifecycle
  // ---------------------------------------------------------------------------

  @Test
  void deleteChat_returnsNoContent() throws Exception {
    UUID chatId = UUID.randomUUID();

    mockMvc
        .perform(delete("/chats/" + chatId).principal(authenticationFor(user)))
        .andExpect(status().isNoContent());

    verify(chatService).deleteChat(any(User.class), eq(chatId));
  }

  @Test
  void addMembers_returnsOk() throws Exception {
    UUID chatId = UUID.randomUUID();
    when(chatService.addMembers(any(User.class), eq(chatId), any(AddChatMembersRequest.class)))
        .thenReturn(chatResponse(chatId, "group"));

    mockMvc
        .perform(
            post("/chats/" + chatId + "/members")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernames\": [\"carol\"]}"))
        .andExpect(status().isOk());
  }

  @Test
  void addMembers_withEmptyUsernames_returnsBadRequest() throws Exception {
    UUID chatId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/chats/" + chatId + "/members")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernames\": []}"))
        .andExpect(status().isBadRequest());

    verify(chatService, never()).addMembers(any(), any(), any());
  }

  @Test
  void removeMember_returnsOk() throws Exception {
    UUID chatId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    when(chatService.removeMember(any(User.class), eq(chatId), eq(memberId)))
        .thenReturn(chatResponse(chatId, "group"));

    mockMvc
        .perform(
            delete("/chats/" + chatId + "/members/" + memberId).principal(authenticationFor(user)))
        .andExpect(status().isOk());
  }

  @Test
  void leaveChat_returnsNoContent() throws Exception {
    UUID chatId = UUID.randomUUID();

    mockMvc
        .perform(delete("/chats/" + chatId + "/members/me").principal(authenticationFor(user)))
        .andExpect(status().isNoContent());

    verify(chatService).leaveChat(any(User.class), eq(chatId));
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private User createUser(String username) {
    User created = new User();
    created.setId(UUID.randomUUID());
    created.setUsername(username);
    return created;
  }

  private UsernamePasswordAuthenticationToken authenticationFor(User principal) {
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  private ChatResponse chatResponse(UUID chatId, String name) {
    return new ChatResponse(
        chatId,
        name,
        null,
        List.of(),
        null,
        LocalDateTime.of(2026, 4, 25, 12, 0),
        false,
        null,
        List.of(),
        null,
        false,
        0,
        0);
  }

  private ChatMessageResponse messageResponse(UUID chatId, String content) {
    return new ChatMessageResponse(
        UUID.randomUUID(),
        chatId,
        null,
        content,
        LocalDateTime.of(2026, 4, 25, 12, 0),
        true,
        false,
        null,
        List.of(),
        List.of(),
        null);
  }
}

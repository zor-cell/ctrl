package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.chat.ChatMessageBroadcaster;
import at.ac.tuwien.inso.ctrl.chat.ChatService;
import at.ac.tuwien.inso.ctrl.chat.dto.AddChatMembersRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.AddReactionRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatMessageResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.CreateChatRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.SendMessageRequest;
import at.ac.tuwien.inso.ctrl.chat.entity.Chat;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMemberId;
import at.ac.tuwien.inso.ctrl.chat.entity.Message;
import at.ac.tuwien.inso.ctrl.chat.entity.Reaction;
import at.ac.tuwien.inso.ctrl.chat.entity.ReactionId;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatMemberRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.MessageRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.ReactionRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.SharedRepository;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link ChatService} business logic and authorization rules. All collaborators are
 * mocked; the focus is on validation branches, ownership/membership checks and reaction handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceUnitTest {

  private static final String EMOJI_THUMBS_UP = "👍";
  private static final String EMOJI_HEART = "❤️";

  @Mock private ChatRepository chatRepository;
  @Mock private ChatMemberRepository chatMemberRepository;
  @Mock private MessageRepository messageRepository;
  @Mock private ReactionRepository reactionRepository;
  @Mock private SharedRepository sharedRepository;
  @Mock private PostRepository postRepository;
  @Mock private UserRepository userRepository;
  @Mock private FriendRelationRepository friendRelationRepository;
  @Mock private FileStorageService fileStorageService;
  @Mock private ChatMessageBroadcaster chatMessageBroadcaster;
  @Mock private EntityManager entityManager;

  @InjectMocks private ChatService chatService;

  private User alice;
  private User bob;

  @BeforeEach
  void setUp() {
    alice = createUser("alice");
    bob = createUser("bob");
    // ChatService uses constructor injection, so Mockito does not field-inject the
    // @PersistenceContext EntityManager. Wire it manually for the tests that need it.
    ReflectionTestUtils.setField(chatService, "entityManager", entityManager);
  }

  // ---------------------------------------------------------------------------
  // getChat / membership
  // ---------------------------------------------------------------------------

  @Test
  void getChat_forUnknownChat_throwsNotFound() {
    UUID chatId = UUID.randomUUID();
    when(chatRepository.findByChatIdWithDetails(chatId)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> chatService.getChat(alice, chatId));
  }

  @Test
  void getChat_forNonMember_throwsForbidden() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, false);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> chatService.getChat(alice, chat.getChatId()));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void markChatAsRead_forNonMember_throwsForbidden() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, false);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> chatService.markChatAsRead(alice, chat.getChatId()));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  // ---------------------------------------------------------------------------
  // createChat
  // ---------------------------------------------------------------------------

  @Test
  void createChat_withSelfUsername_throwsInvalidRequest() {
    CreateChatRequest request = new CreateChatRequest(null, List.of(alice.getUsername()));

    assertThrows(InvalidRequestException.class, () -> chatService.createChat(alice, request));
  }

  @Test
  void createChat_withBlankUsernames_throwsInvalidRequest() {
    CreateChatRequest request = new CreateChatRequest(null, List.of("   "));

    assertThrows(InvalidRequestException.class, () -> chatService.createChat(alice, request));
  }

  @Test
  void createChat_withUnknownUser_throwsNotFound() {
    when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());
    CreateChatRequest request = new CreateChatRequest(null, List.of("bob"));

    assertThrows(NotFoundException.class, () -> chatService.createChat(alice, request));
  }

  @Test
  void createChat_withNonFriend_throwsForbidden() {
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    when(friendRelationRepository.existsByUsers(any(User.class), any(User.class)))
        .thenReturn(false);
    CreateChatRequest request = new CreateChatRequest(null, List.of("bob"));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> chatService.createChat(alice, request));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void createChat_groupWithoutName_throwsInvalidRequest() {
    User carol = createUser("carol");
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    when(userRepository.findByUsername("carol")).thenReturn(Optional.of(carol));
    when(friendRelationRepository.existsByUsers(any(User.class), any(User.class))).thenReturn(true);
    CreateChatRequest request = new CreateChatRequest(null, List.of("bob", "carol"));

    assertThrows(InvalidRequestException.class, () -> chatService.createChat(alice, request));
  }

  @Test
  void createChat_groupWithInvalidName_throwsInvalidRequest() {
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    when(friendRelationRepository.existsByUsers(any(User.class), any(User.class))).thenReturn(true);
    CreateChatRequest request = new CreateChatRequest("invalid name!", List.of("bob"));

    assertThrows(InvalidRequestException.class, () -> chatService.createChat(alice, request));
  }

  @Test
  void createChat_directWithExistingChat_isReusedAndReactivated() {
    Chat existing = directChat(alice, bob);
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    when(friendRelationRepository.existsByUsers(any(User.class), any(User.class))).thenReturn(true);
    when(chatRepository.findDirectChatsBetweenUsers(alice.getId(), bob.getId()))
        .thenReturn(List.of(existing));
    when(chatRepository.findByChatIdWithDetails(existing.getChatId()))
        .thenReturn(Optional.of(existing));

    ChatMember leftMembership =
        ChatMember.builder()
            .id(new ChatMemberId(existing.getChatId(), alice.getId()))
            .chat(existing)
            .user(alice)
            .joinedAt(LocalDateTime.now().minusDays(2))
            .leftAt(LocalDateTime.now().minusDays(1))
            .build();
    when(chatMemberRepository.findById(any(ChatMemberId.class)))
        .thenReturn(Optional.of(leftMembership));
    stubResponseMappers();

    CreateChatRequest request = new CreateChatRequest(null, List.of("bob"));
    var response = chatService.createChat(alice, request);

    assertNotNull(response);
    assertEquals(existing.getChatId(), response.chatId());
    verify(chatRepository, never()).save(any(Chat.class));
    verify(chatMemberRepository, atLeastOnce()).save(any(ChatMember.class));
  }

  // ---------------------------------------------------------------------------
  // sendMessage
  // ---------------------------------------------------------------------------

  @Test
  void sendMessage_withoutContentOrSharedPost_throwsInvalidRequest() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    SendMessageRequest request = new SendMessageRequest("   ", null);

    assertThrows(
        InvalidRequestException.class,
        () -> chatService.sendMessage(alice, chat.getChatId(), request));
  }

  @Test
  void sendMessage_persistsMessageAndBroadcasts() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();
    when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

    SendMessageRequest request = new SendMessageRequest("hello there", null);
    ChatMessageResponse response = chatService.sendMessage(alice, chat.getChatId(), request);

    assertEquals("hello there", response.content());
    verify(messageRepository).save(any(Message.class));
    verify(chatMessageBroadcaster, atLeastOnce())
        .publishMessageEvent(any(), eq(chat.getChatId()), any());
  }

  // ---------------------------------------------------------------------------
  // reactToMessage
  // ---------------------------------------------------------------------------

  @Test
  void reactToMessage_withUnsupportedEmoji_throwsInvalidRequest() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    AddReactionRequest request = new AddReactionRequest("🤡");

    assertThrows(
        InvalidRequestException.class,
        () -> chatService.reactToMessage(alice, chat.getChatId(), UUID.randomUUID(), request));
  }

  @Test
  void reactToMessage_onSystemMessage_throwsInvalidRequest() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Message systemMessage = message(chat, bob, true);
    when(messageRepository.findById(systemMessage.getMessageId()))
        .thenReturn(Optional.of(systemMessage));
    AddReactionRequest request = new AddReactionRequest(EMOJI_THUMBS_UP);

    assertThrows(
        InvalidRequestException.class,
        () ->
            chatService.reactToMessage(
                alice, chat.getChatId(), systemMessage.getMessageId(), request));
  }

  @Test
  void reactToMessage_withNewEmoji_savesReactionAndBroadcastsUpdate() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();
    Message message = message(chat, bob, false);
    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(messageRepository.findWithDetailsByMessageId(message.getMessageId()))
        .thenReturn(Optional.of(message));
    when(reactionRepository.findById(any(ReactionId.class))).thenReturn(Optional.empty());

    AddReactionRequest request = new AddReactionRequest(EMOJI_THUMBS_UP);
    chatService.reactToMessage(alice, chat.getChatId(), message.getMessageId(), request);

    verify(reactionRepository).save(any(Reaction.class));
    verify(reactionRepository, never()).delete(any(Reaction.class));
    verify(chatMessageBroadcaster, atLeastOnce())
        .publishMessageEvent(any(), eq(chat.getChatId()), any());
  }

  @Test
  void reactToMessage_withSameEmoji_togglesReactionOff() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();
    Message message = message(chat, bob, false);
    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(messageRepository.findWithDetailsByMessageId(message.getMessageId()))
        .thenReturn(Optional.of(message));
    Reaction existing = reaction(message, alice, EMOJI_THUMBS_UP);
    when(reactionRepository.findById(any(ReactionId.class))).thenReturn(Optional.of(existing));

    AddReactionRequest request = new AddReactionRequest(EMOJI_THUMBS_UP);
    chatService.reactToMessage(alice, chat.getChatId(), message.getMessageId(), request);

    verify(reactionRepository).delete(existing);
    verify(reactionRepository, never()).save(any(Reaction.class));
  }

  @Test
  void reactToMessage_withDifferentEmoji_updatesReaction() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();
    Message message = message(chat, bob, false);
    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(messageRepository.findWithDetailsByMessageId(message.getMessageId()))
        .thenReturn(Optional.of(message));
    Reaction existing = reaction(message, alice, EMOJI_HEART);
    when(reactionRepository.findById(any(ReactionId.class))).thenReturn(Optional.of(existing));

    AddReactionRequest request = new AddReactionRequest(EMOJI_THUMBS_UP);
    chatService.reactToMessage(alice, chat.getChatId(), message.getMessageId(), request);

    assertEquals(EMOJI_THUMBS_UP, existing.getContent());
    verify(reactionRepository).save(existing);
    verify(reactionRepository, never()).delete(any(Reaction.class));
  }

  // ---------------------------------------------------------------------------
  // deleteChat / leaveChat / removeMember / addMembers (authorization)
  // ---------------------------------------------------------------------------

  @Test
  void deleteChat_onDirectChat_throwsInvalidRequest() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);

    assertThrows(
        InvalidRequestException.class, () -> chatService.deleteChat(alice, chat.getChatId()));
  }

  @Test
  void deleteChat_byNonOwner_throwsForbidden() {
    Chat chat = groupChat(bob, alice, bob);
    stubChatMembership(chat, true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> chatService.deleteChat(alice, chat.getChatId()));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void leaveChat_asGroupOwner_throwsInvalidRequest() {
    Chat chat = groupChat(alice, alice, bob);
    stubChatMembership(chat, true);

    assertThrows(
        InvalidRequestException.class, () -> chatService.leaveChat(alice, chat.getChatId()));
  }

  @Test
  void removeMember_byNonOwner_throwsForbidden() {
    Chat chat = groupChat(bob, alice, bob);
    stubChatMembership(chat, true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> chatService.removeMember(alice, chat.getChatId(), bob.getId()));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void removeMember_ownerRemovingSelf_throwsInvalidRequest() {
    Chat chat = groupChat(alice, alice, bob);
    stubChatMembership(chat, true);

    assertThrows(
        InvalidRequestException.class,
        () -> chatService.removeMember(alice, chat.getChatId(), alice.getId()));
  }

  @Test
  void addMembers_byNonOwner_throwsForbidden() {
    Chat chat = groupChat(bob, alice, bob);
    stubChatMembership(chat, true);
    AddChatMembersRequest request = new AddChatMembersRequest(List.of("carol"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> chatService.addMembers(alice, chat.getChatId(), request));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void addMembers_whenAllAlreadyMembers_throwsInvalidRequest() {
    Chat chat = groupChat(alice, alice, bob);
    stubChatMembership(chat, true);
    when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    when(friendRelationRepository.existsByUsers(any(User.class), any(User.class))).thenReturn(true);
    AddChatMembersRequest request = new AddChatMembersRequest(List.of("bob"));

    assertThrows(
        InvalidRequestException.class,
        () -> chatService.addMembers(alice, chat.getChatId(), request));
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private void stubChatMembership(Chat chat, boolean active) {
    when(chatRepository.findByChatIdWithDetails(chat.getChatId())).thenReturn(Optional.of(chat));
    when(chatMemberRepository.existsActiveByChatChatIdAndUser(
            eq(chat.getChatId()), any(User.class)))
        .thenReturn(active);
  }

  private void stubResponseMappers() {
    when(messageRepository.findTopByChatOrderByDateDesc(any())).thenReturn(Optional.empty());
    when(messageRepository.countPersonalMessages(any(), any())).thenReturn(0L);
    when(messageRepository.countUnreadMessagesSince(any(), any(), any())).thenReturn(0L);
    when(messageRepository.countUnreadMessagesWhenNeverRead(any(), any())).thenReturn(0L);
  }

  private User createUser(String username) {
    return User.builder()
        .id(UUID.randomUUID())
        .username(username)
        .name(username)
        .password("password")
        .build();
  }

  private Chat directChat(User... members) {
    return buildChat(null, members);
  }

  private Chat groupChat(User owner, User... members) {
    return buildChat(owner, members);
  }

  private Chat buildChat(User owner, User... members) {
    UUID chatId = UUID.randomUUID();
    Chat chat =
        Chat.builder()
            .chatId(chatId)
            .name(owner != null ? "group" : null)
            .createdAt(LocalDateTime.now().minusHours(1))
            .owner(owner)
            .members(new ArrayList<>())
            .build();

    LocalDateTime now = LocalDateTime.now();
    for (User member : members) {
      chat.getMembers()
          .add(
              ChatMember.builder()
                  .id(new ChatMemberId(chatId, member.getId()))
                  .chat(chat)
                  .user(member)
                  .joinedAt(now)
                  .lastReadAt(now)
                  .build());
    }
    return chat;
  }

  private Message message(Chat chat, User from, boolean system) {
    return Message.builder()
        .messageId(UUID.randomUUID())
        .chat(chat)
        .fromUser(from)
        .content(system ? "" : "hello")
        .date(LocalDateTime.now())
        .system(system)
        .build();
  }

  private Reaction reaction(Message message, User from, String emoji) {
    return Reaction.builder()
        .id(new ReactionId(from.getId(), message.getMessageId()))
        .message(message)
        .fromUser(from)
        .content(emoji)
        .createdAt(LocalDateTime.now())
        .build();
  }
}

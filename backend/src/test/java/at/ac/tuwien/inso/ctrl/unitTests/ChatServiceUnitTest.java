package at.ac.tuwien.inso.ctrl.unitTests;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import at.ac.tuwien.inso.ctrl.chat.entity.Shared;
import at.ac.tuwien.inso.ctrl.chat.entity.SystemEventType;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatMemberRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.MessageRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.ReactionRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.SharedRepository;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.BlockService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
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
  private static final String EMOJI_FIRE = "🔥";

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
  @Mock private BlockService blockService;
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
  void getChat_whenStoredGroupOwnerIsDeleted_treatsActiveMembersAsOwners() {
    User deletedOwner = createUser("deletedOwner");
    deletedOwner.setDeleted(true);
    Chat chat = groupChat(deletedOwner, deletedOwner, alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();

    var response = chatService.getChat(alice, chat.getChatId());

    assertTrue(response.owner());
    assertEquals(Set.of(alice.getId(), bob.getId()), Set.copyOf(response.ownerIds()));
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

  @Test
  void getChats_loadsActiveChatsWithDetailsInSingleRepositoryCall() {
    Chat chat = directChat(alice, bob);
    when(chatRepository.findActiveChatsForUserWithDetails(alice.getId())).thenReturn(List.of(chat));
    stubResponseMappers();

    var response = chatService.getChats(alice);

    assertEquals(1, response.size());
    verify(chatRepository).findActiveChatsForUserWithDetails(alice.getId());
    verify(chatMemberRepository, never()).findChatIdsForUser(any());
    verify(chatRepository, never()).findByChatIdWithDetails(any());
  }

  @Test
  void getChats_excludesDirectChatsWithBlockedUsers() {
    Chat chat = directChat(alice, bob);
    when(chatRepository.findActiveChatsForUserWithDetails(alice.getId())).thenReturn(List.of(chat));
    when(blockService.inBlockedRelationship(alice, bob)).thenReturn(true);

    var response = chatService.getChats(alice);

    assertTrue(response.isEmpty());
    verify(messageRepository, never()).findTopByChatOrderByDateDesc(any());
  }

  @Test
  void getChats_includesDirectChatAgainWhenBlockedRelationshipIsGone() {
    Chat chat = directChat(alice, bob);
    when(chatRepository.findActiveChatsForUserWithDetails(alice.getId())).thenReturn(List.of(chat));
    when(blockService.inBlockedRelationship(alice, bob)).thenReturn(false);
    stubResponseMappers();

    var response = chatService.getChats(alice);

    assertEquals(1, response.size());
    assertEquals(chat.getChatId(), response.getFirst().chatId());
  }

  @Test
  void getMessages_loadsLatestPageAndReturnsAscendingMessages() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Message olderMessage = message(chat, bob, false);
    olderMessage.setDate(LocalDateTime.parse("2026-06-12T10:00:00"));
    Message newerMessage = message(chat, alice, false);
    newerMessage.setDate(LocalDateTime.parse("2026-06-12T10:01:00"));
    when(messageRepository.findLatestMessageIds(chat, PageRequest.of(0, 30)))
        .thenReturn(List.of(newerMessage.getMessageId(), olderMessage.getMessageId()));
    when(messageRepository.findMessagesWithDetailsByIds(
            List.of(newerMessage.getMessageId(), olderMessage.getMessageId())))
        .thenReturn(List.of(newerMessage, olderMessage));

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals(
        List.of(olderMessage.getMessageId(), newerMessage.getMessageId()),
        response.stream().map(ChatMessageResponse::messageId).toList());
    verify(messageRepository).findLatestMessageIds(chat, PageRequest.of(0, 30));
    verify(messageRepository, never()).findByChatOrderByDateAsc(any());
  }

  @Test
  void getMessages_withBeforeCursorLoadsOlderPage() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    LocalDateTime before = LocalDateTime.parse("2026-06-12T10:02:00");
    Message olderMessage = message(chat, bob, false);
    olderMessage.setDate(LocalDateTime.parse("2026-06-12T10:01:00"));
    when(messageRepository.findMessageIdsBefore(chat, before, PageRequest.of(0, 15)))
        .thenReturn(List.of(olderMessage.getMessageId()));
    when(messageRepository.findMessagesWithDetailsByIds(List.of(olderMessage.getMessageId())))
        .thenReturn(List.of(olderMessage));

    List<ChatMessageResponse> response =
        chatService.getMessages(alice, chat.getChatId(), "2026-06-12T10:02:00Z", 15);

    assertEquals(
        List.of(olderMessage.getMessageId()),
        response.stream().map(ChatMessageResponse::messageId).toList());
    verify(messageRepository).findMessageIdsBefore(chat, before, PageRequest.of(0, 15));
    verify(messageRepository, never()).findLatestMessageIds(any(), any());
  }

  @Test
  void getMessages_withInvalidBeforeCursor_throwsInvalidRequest() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);

    assertThrows(
        InvalidRequestException.class,
        () -> chatService.getMessages(alice, chat.getChatId(), "not-a-date", 15));
    verify(messageRepository, never()).findLatestMessageIds(any(), any());
    verify(messageRepository, never()).findMessageIdsBefore(any(), any(), any());
  }

  @Test
  void getMessages_withLimitOutsideBounds_clampsPageSize() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    when(messageRepository.findLatestMessageIds(chat, PageRequest.of(0, 1))).thenReturn(List.of());
    when(messageRepository.findMessageIdsBefore(
            eq(chat), any(LocalDateTime.class), eq(PageRequest.of(0, 100))))
        .thenReturn(List.of());

    chatService.getMessages(alice, chat.getChatId(), null, -5);
    chatService.getMessages(alice, chat.getChatId(), "2026-06-12T10:02:00", 500);

    verify(messageRepository).findLatestMessageIds(chat, PageRequest.of(0, 1));
    verify(messageRepository)
        .findMessageIdsBefore(eq(chat), any(LocalDateTime.class), eq(PageRequest.of(0, 100)));
  }

  @Test
  void getMessages_withPublicSharedPost_returnsVisibleSharedPost() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Post publicPost = post("post-public", bob, true);
    Message message = messageWithSharedPost(chat, bob, publicPost);
    stubSingleMessagePage(chat, message);

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals(true, response.getFirst().sharedPost().canView());
    assertEquals("shared caption", response.getFirst().sharedPost().caption());
    assertEquals("image", response.getFirst().sharedPost().mediaKind());
    assertEquals(null, response.getFirst().sharedPost().unavailableReason());
  }

  @Test
  void getMessages_withPrivateSharedPostViewerIsOwner_returnsVisibleSharedPost() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Post privatePost = post("post-own-private", alice, false);
    Message message = messageWithSharedPost(chat, alice, privatePost);
    stubSingleMessagePage(chat, message);

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals(true, response.getFirst().sharedPost().canView());
    assertEquals("shared caption", response.getFirst().sharedPost().caption());
    verify(friendRelationRepository, never()).existsByUsers(alice, alice);
  }

  @Test
  void getMessages_withPrivateSharedPostViewerIsFriend_returnsVisibleSharedPost() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Post privatePost = post("post-friend-private", bob, false);
    Message message = messageWithSharedPost(chat, bob, privatePost);
    stubSingleMessagePage(chat, message);
    when(friendRelationRepository.existsByUsers(bob, alice)).thenReturn(true);

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals(true, response.getFirst().sharedPost().canView());
    assertEquals("post-friend-private", response.getFirst().sharedPost().postId());
    assertEquals(null, response.getFirst().sharedPost().unavailableReason());
  }

  @Test
  void getMessages_withSharedPostThatWasRemoved_returnsUnavailableSharedPost() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Post removedPost = post("post-removed", bob, true);
    removedPost.setIsRemoved(true);
    Shared shared = shared(removedPost, bob);
    Message message = message(chat, bob, false);
    message.setShared(shared);
    when(messageRepository.findLatestMessageIds(chat, PageRequest.of(0, 30)))
        .thenReturn(List.of(message.getMessageId()));
    when(messageRepository.findMessagesWithDetailsByIds(List.of(message.getMessageId())))
        .thenReturn(List.of(message));

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals(1, response.size());
    assertEquals("post-removed", response.getFirst().sharedPost().postId());
    assertEquals("REMOVED", response.getFirst().sharedPost().unavailableReason());
    assertEquals(false, response.getFirst().sharedPost().canView());
    assertEquals(null, response.getFirst().sharedPost().caption());
    assertEquals(null, response.getFirst().sharedPost().mediaRef());
  }

  @Test
  void getMessages_withPrivateSharedPostViewerCannotAccess_returnsUnavailableSharedPost() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Post privatePost = post("post-private", bob, false);
    Shared shared = shared(privatePost, bob);
    Message message = message(chat, bob, false);
    message.setShared(shared);
    when(friendRelationRepository.existsByUsers(bob, alice)).thenReturn(false);
    when(messageRepository.findLatestMessageIds(chat, PageRequest.of(0, 30)))
        .thenReturn(List.of(message.getMessageId()));
    when(messageRepository.findMessagesWithDetailsByIds(List.of(message.getMessageId())))
        .thenReturn(List.of(message));

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals(1, response.size());
    assertEquals("post-private", response.getFirst().sharedPost().postId());
    assertEquals("PRIVATE", response.getFirst().sharedPost().unavailableReason());
    assertEquals(false, response.getFirst().sharedPost().canView());
    assertEquals(false, response.getFirst().sharedPost().publicPost());
    assertEquals(null, response.getFirst().sharedPost().caption());
    assertEquals(null, response.getFirst().sharedPost().mediaRef());
  }

  @Test
  void getMessages_withSharedPostHardDeleted_returnsRemovedUnavailableSharedPost() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Shared shared = shared(null, bob);
    Message message = message(chat, bob, false);
    message.setShared(shared);
    when(messageRepository.findLatestMessageIds(chat, PageRequest.of(0, 30)))
        .thenReturn(List.of(message.getMessageId()));
    when(messageRepository.findMessagesWithDetailsByIds(List.of(message.getMessageId())))
        .thenReturn(List.of(message));

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals(1, response.size());
    assertEquals(shared.getSharedId(), response.getFirst().sharedPost().sharedId());
    assertEquals(null, response.getFirst().sharedPost().postId());
    assertEquals("REMOVED", response.getFirst().sharedPost().unavailableReason());
    assertEquals(false, response.getFirst().sharedPost().canView());
  }

  @Test
  void getMessages_withMemberAddedByCurrentUser_formatsYouAddedTarget() {
    User carol = createUser("carol");
    Chat chat = groupChat(alice, alice, bob, carol);
    Message message = systemMessage(chat, alice, SystemEventType.MEMBER_ADDED, carol);
    stubChatMembership(chat, true);
    stubSingleMessagePage(chat, message);
    when(userRepository.findById(carol.getId())).thenReturn(Optional.of(carol));

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals("You added carol", response.getFirst().content());
  }

  @Test
  void getMessages_withCurrentUserAddedByOtherMember_formatsAddedYou() {
    Chat chat = groupChat(bob, alice, bob);
    Message message = systemMessage(chat, bob, SystemEventType.MEMBER_ADDED, alice);
    stubChatMembership(chat, true);
    stubSingleMessagePage(chat, message);
    when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals("bob added you", response.getFirst().content());
  }

  @Test
  void getMessages_withMemberRemovedByOtherMember_formatsRemovedTarget() {
    User carol = createUser("carol");
    Chat chat = groupChat(bob, alice, bob, carol);
    Message message = systemMessage(chat, bob, SystemEventType.MEMBER_REMOVED, carol);
    stubChatMembership(chat, true);
    stubSingleMessagePage(chat, message);
    when(userRepository.findById(carol.getId())).thenReturn(Optional.of(carol));

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals("bob removed carol", response.getFirst().content());
  }

  @Test
  void getMessages_withMemberLeft_formatsActorLeftGroup() {
    Chat chat = groupChat(alice, alice, bob);
    Message message = systemMessage(chat, bob, SystemEventType.MEMBER_LEFT);
    stubChatMembership(chat, true);
    stubSingleMessagePage(chat, message);

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals("bob left the group", response.getFirst().content());
  }

  @Test
  void getMessages_withDeletedSystemActor_usesDeletedUserLabel() {
    User deletedUser = createUser("deleted-user");
    deletedUser.setDeleted(true);
    Chat chat = groupChat(alice, alice, bob, deletedUser);
    Message message = systemMessage(chat, deletedUser, SystemEventType.MEMBER_LEFT);
    stubChatMembership(chat, true);
    stubSingleMessagePage(chat, message);

    List<ChatMessageResponse> response = chatService.getMessages(alice, chat.getChatId());

    assertEquals("Deleted user left the group", response.getFirst().content());
    assertEquals("Deleted user", response.getFirst().sender().username());
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
    verify(messageRepository)
        .countPersonalMessagesByUsers(eq(chat), eq(Set.of(alice.getId(), bob.getId())));
    verify(messageRepository)
        .countUnreadMessagesByUsers(eq(chat), eq(Set.of(alice.getId(), bob.getId())));
    verify(messageRepository, never()).findTopByChatOrderByDateDesc(chat);
    verify(chatMessageBroadcaster, atLeastOnce())
        .publishMessageEvent(any(), eq(chat.getChatId()), any());
  }

  @Test
  void sendMessage_inBlockedDirectChat_throwsForbiddenAndDoesNotBroadcast() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    when(blockService.inBlockedRelationship(alice, bob)).thenReturn(true);
    SendMessageRequest request = new SendMessageRequest("hello?", null);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> chatService.sendMessage(alice, chat.getChatId(), request));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    verify(messageRepository, never()).save(any(Message.class));
    verify(chatMessageBroadcaster, never()).publishMessageEvent(any(), any(), any());
  }

  @Test
  void sendMessage_withPrivatePostUserCannotView_throwsForbiddenAndDoesNotSave() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    Post privatePost = post("post-private-share", bob, false);
    when(postRepository.findById("post-private-share")).thenReturn(Optional.of(privatePost));
    when(friendRelationRepository.existsByUsers(bob, alice)).thenReturn(false);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                chatService.sendMessage(
                    alice, chat.getChatId(), new SendMessageRequest(null, "post-private-share")));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    verify(sharedRepository, never()).save(any(Shared.class));
    verify(messageRepository, never()).save(any(Message.class));
  }

  @Test
  void sendMessage_withVisiblePostToShare_persistsSharedPostAndBroadcasts() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();
    Post publicPost = post("post-share", bob, true);
    when(postRepository.findById("post-share")).thenReturn(Optional.of(publicPost));
    when(sharedRepository.save(any(Shared.class))).thenAnswer(inv -> inv.getArgument(0));
    when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

    ChatMessageResponse response =
        chatService.sendMessage(
            alice, chat.getChatId(), new SendMessageRequest("look", "post-share"));

    assertEquals("look", response.content());
    assertEquals("post-share", response.sharedPost().postId());
    assertEquals(true, response.sharedPost().canView());
    verify(sharedRepository).save(any(Shared.class));
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
    when(reactionRepository.findAllByUserIdAndMessageId(alice.getId(), message.getMessageId()))
        .thenReturn(List.of());

    AddReactionRequest request = new AddReactionRequest(EMOJI_THUMBS_UP);
    chatService.reactToMessage(alice, chat.getChatId(), message.getMessageId(), request);

    verify(reactionRepository).save(any(Reaction.class));
    verify(reactionRepository, never()).deleteAllByUserIdAndMessageId(any(), any());
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
    when(reactionRepository.findAllByUserIdAndMessageId(alice.getId(), message.getMessageId()))
        .thenReturn(List.of(existing));

    AddReactionRequest request = new AddReactionRequest(EMOJI_THUMBS_UP);
    chatService.reactToMessage(alice, chat.getChatId(), message.getMessageId(), request);

    verify(reactionRepository).deleteAllByUserIdAndMessageId(alice.getId(), message.getMessageId());
    verify(reactionRepository, never()).save(any(Reaction.class));
  }

  @Test
  void reactToMessage_withDifferentEmoji_replacesExistingReaction() {
    Chat chat = directChat(alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();
    Message message = message(chat, bob, false);
    when(messageRepository.findById(message.getMessageId())).thenReturn(Optional.of(message));
    when(messageRepository.findWithDetailsByMessageId(message.getMessageId()))
        .thenReturn(Optional.of(message));
    Reaction existing = reaction(message, alice, EMOJI_FIRE);
    when(reactionRepository.findAllByUserIdAndMessageId(alice.getId(), message.getMessageId()))
        .thenReturn(List.of(existing));

    AddReactionRequest request = new AddReactionRequest(EMOJI_THUMBS_UP);
    chatService.reactToMessage(alice, chat.getChatId(), message.getMessageId(), request);

    verify(reactionRepository).deleteAllByUserIdAndMessageId(alice.getId(), message.getMessageId());
    verify(reactionRepository).save(any(Reaction.class));
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

  @Test
  void addMembers_withNewFriend_addsMemberAndCreatesSystemMessage() {
    User carol = createUser("carol");
    Chat chat = groupChat(alice, alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();
    when(userRepository.findByUsername("carol")).thenReturn(Optional.of(carol));
    when(friendRelationRepository.existsByUsers(alice, carol)).thenReturn(true);
    when(chatRepository.findByChatIdWithDetails(chat.getChatId())).thenReturn(Optional.of(chat));
    when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
    when(messageRepository.findWithDetailsByMessageId(any()))
        .thenAnswer(
            inv ->
                Optional.of(
                    Message.builder()
                        .messageId(inv.getArgument(0))
                        .chat(chat)
                        .fromUser(alice)
                        .content("")
                        .date(LocalDateTime.now())
                        .system(true)
                        .build()));

    var response =
        chatService.addMembers(
            alice, chat.getChatId(), new AddChatMembersRequest(List.of("carol")));

    assertEquals(chat.getChatId(), response.chatId());
    assertTrue(chat.getMembers().stream().anyMatch(member -> member.getUser().equals(carol)));
    verify(messageRepository).save(any(Message.class));
    verify(chatMessageBroadcaster, atLeastOnce())
        .publishMessageEvent(any(), eq(chat.getChatId()), any());
  }

  @Test
  void removeMember_asOwner_removesMemberAndCreatesSystemMessage() {
    Chat chat = groupChat(alice, alice, bob);
    stubChatMembership(chat, true);
    stubResponseMappers();
    stubSystemMessagePersistence(chat);
    when(chatMemberRepository.deleteByChatIdAndUserId(chat.getChatId(), bob.getId())).thenReturn(1);

    var response = chatService.removeMember(alice, chat.getChatId(), bob.getId());

    assertEquals(chat.getChatId(), response.chatId());
    verify(chatMemberRepository).deleteByChatIdAndUserId(chat.getChatId(), bob.getId());
    verify(chatMessageBroadcaster).publishChatRemoved(bob.getUsername(), chat.getChatId());
    verify(messageRepository).save(any(Message.class));
  }

  @Test
  void leaveChat_asGroupMember_removesMemberAndCreatesSystemMessage() {
    Chat chat = groupChat(alice, alice, bob);
    stubChatMembership(chat, true);
    stubSystemMessagePersistence(chat);

    chatService.leaveChat(bob, chat.getChatId());

    verify(chatMemberRepository).deleteByChatIdAndUserId(chat.getChatId(), bob.getId());
    verify(chatMessageBroadcaster).publishChatRemoved(bob.getUsername(), chat.getChatId());
    verify(messageRepository).save(any(Message.class));
  }

  @Test
  void leaveChat_onDirectChat_marksCurrentUserLeft() {
    Chat chat = directChat(alice, bob);
    ChatMember aliceMember = chat.getMembers().getFirst();
    stubChatMembership(chat, true);
    when(userRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
    when(chatMemberRepository.findByChatChatIdAndUser(chat.getChatId(), alice))
        .thenReturn(Optional.of(aliceMember));

    chatService.leaveChat(alice, chat.getChatId());

    assertNotNull(aliceMember.getLeftAt());
    verify(chatMemberRepository).save(aliceMember);
    verify(chatMessageBroadcaster).publishChatRemoved(alice.getUsername(), chat.getChatId());
  }

  @Test
  void createAccountDeletedSystemMessages_forEmptyDirectChat_deletesChat() {
    bob.setDeleted(true);
    Chat chat = directChat(alice, bob);
    when(chatRepository.findActiveChatsForUserWithDetails(bob.getId())).thenReturn(List.of(chat));
    when(messageRepository.countByChat(chat)).thenReturn(0L);

    chatService.createAccountDeletedSystemMessages(bob);

    verify(chatMessageBroadcaster).publishChatRemoved(alice.getUsername(), chat.getChatId());
    verify(chatRepository).delete(chat);
    verify(messageRepository, never()).save(any(Message.class));
  }

  @Test
  void createAccountDeletedSystemMessages_forDirectChatWithMessages_marksDeletedUserLeft() {
    bob.setDeleted(true);
    Chat chat = directChat(alice, bob);
    ChatMember bobMember = chat.getMembers().get(1);
    when(chatRepository.findActiveChatsForUserWithDetails(bob.getId())).thenReturn(List.of(chat));
    when(messageRepository.countByChat(chat)).thenReturn(1L);
    when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
    when(userRepository.getReferenceById(bob.getId())).thenReturn(bob);
    when(chatMemberRepository.findByChatChatIdAndUser(chat.getChatId(), bob))
        .thenReturn(Optional.of(bobMember));
    stubResponseMappers();
    stubSystemMessagePersistence(chat);

    chatService.createAccountDeletedSystemMessages(bob);

    assertNotNull(bobMember.getLeftAt());
    verify(chatMemberRepository).save(bobMember);
    verify(messageRepository).save(any(Message.class));
    verify(chatRepository, never()).delete(chat);
  }

  @Test
  void createAccountDeletedSystemMessages_whenGroupOwnerDeletesAccount_promotesRemainingMembers() {
    bob.setDeleted(true);
    Chat chat = groupChat(bob, bob, alice);
    ChatMember bobMember = chat.getMembers().getFirst();
    when(chatRepository.findActiveChatsForUserWithDetails(bob.getId())).thenReturn(List.of(chat));
    when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
    when(userRepository.getReferenceById(bob.getId())).thenReturn(bob);
    when(chatMemberRepository.findByChatChatIdAndUser(chat.getChatId(), bob))
        .thenReturn(Optional.of(bobMember));
    when(chatMemberRepository.existsActiveByChatChatIdAndUser(
            eq(chat.getChatId()), any(User.class)))
        .thenReturn(true);
    stubResponseMappers();
    stubSystemMessagePersistence(chat);

    chatService.createAccountDeletedSystemMessages(bob);

    assertNotNull(bobMember.getLeftAt());
    verify(messageRepository, atLeastOnce()).save(any(Message.class));
    var response = chatService.getChat(alice, chat.getChatId());
    assertTrue(response.owner());
    assertEquals(List.of(alice.getId()), response.ownerIds());
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

  private Message systemMessage(Chat chat, User from, SystemEventType event, User... targetUsers) {
    Message message = message(chat, from, true);
    message.setSystemEvent(event);
    message.setSystemTargetUserIds(
        Arrays.stream(targetUsers).map(User::getId).map(UUID::toString).collect(joining(",")));
    return message;
  }

  private Message messageWithSharedPost(Chat chat, User from, Post post) {
    Message message = message(chat, from, false);
    message.setShared(shared(post, from));
    return message;
  }

  private void stubSingleMessagePage(Chat chat, Message message) {
    when(messageRepository.findLatestMessageIds(chat, PageRequest.of(0, 30)))
        .thenReturn(List.of(message.getMessageId()));
    when(messageRepository.findMessagesWithDetailsByIds(List.of(message.getMessageId())))
        .thenReturn(List.of(message));
  }

  private void stubSystemMessagePersistence(Chat chat) {
    when(chatRepository.findByChatIdWithDetails(chat.getChatId())).thenReturn(Optional.of(chat));
    when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
    when(messageRepository.findWithDetailsByMessageId(any()))
        .thenAnswer(
            inv ->
                Optional.of(
                    Message.builder()
                        .messageId(inv.getArgument(0))
                        .chat(chat)
                        .fromUser(alice)
                        .content("")
                        .date(LocalDateTime.now())
                        .system(true)
                        .build()));
  }

  private Reaction reaction(Message message, User from, String emoji) {
    return Reaction.builder()
        .id(new ReactionId(from.getId(), message.getMessageId(), emoji))
        .message(message)
        .fromUser(from)
        .content(emoji)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private Post post(String postId, User owner, boolean isPublic) {
    return Post.builder()
        .postId(postId)
        .user(owner)
        .caption("shared caption")
        .mediaRef("posts/media/" + postId + ".png")
        .isPublic(isPublic)
        .isRemoved(false)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private Shared shared(Post post, User sharedBy) {
    return Shared.builder()
        .sharedId(UUID.randomUUID())
        .post(post)
        .sharedBy(sharedBy)
        .createdAt(LocalDateTime.now())
        .build();
  }
}

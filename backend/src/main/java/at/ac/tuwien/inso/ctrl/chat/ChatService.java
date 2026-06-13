package at.ac.tuwien.inso.ctrl.chat;

import at.ac.tuwien.inso.ctrl.chat.dto.AddChatMembersRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.AddReactionRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatMessageResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatParticipantResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatRealtimePayload;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.CreateChatRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.MessageReactionResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.SendMessageRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.SharedPostResponse;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatService {

  private static final int MAX_GROUP_MEMBERS = 50;
  private static final int DEFAULT_MESSAGE_PAGE_SIZE = 30;
  private static final int MAX_MESSAGE_PAGE_SIZE = 100;
  private static final Pattern GROUP_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9._]+$");
  private static final Set<String> ALLOWED_REACTION_EMOJIS =
      Set.of("👍", "❤️", "😂", "😮", "😢", "😡", "🎉", "🔥");

  private final ChatRepository chatRepository;
  private final ChatMemberRepository chatMemberRepository;
  private final MessageRepository messageRepository;
  private final ReactionRepository reactionRepository;
  private final SharedRepository sharedRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final FriendRelationRepository friendRelationRepository;
  private final FileStorageService fileStorageService;
  private final ChatMessageBroadcaster chatMessageBroadcaster;

  @PersistenceContext private EntityManager entityManager;

  @Transactional(readOnly = true)
  public ChatResponse getChat(User currentUser, UUID chatId) {
    return toChatResponse(requireChatMember(currentUser, chatId), currentUser);
  }

  @Transactional(readOnly = true)
  public List<ChatResponse> getChats(User currentUser) {
    List<ChatResponse> chats =
        loadChatsForUser(currentUser).stream()
            .map(chat -> toChatResponse(chat, currentUser))
            .sorted(
                Comparator.comparing(
                        (ChatResponse chat) -> {
                          if (chat.lastMessage() != null) {
                            return chat.lastMessage().date();
                          }
                          return chat.createdAt();
                        })
                    .reversed())
            .toList();

    return dedupeDirectChats(chats, currentUser).stream()
        .filter(chat -> chat.groupChat() || hasVisibleDirectPartner(chat, currentUser))
        .toList();
  }

  @Transactional
  public ChatResponse createChat(User currentUser, CreateChatRequest request) {
    List<User> invitedUsers = resolveInvitedUsers(currentUser, request.usernames());
    String chatName = trimToNull(request.name());
    boolean groupChat = invitedUsers.size() > 1 || chatName != null;

    if (!groupChat) {
      User invitedUser = invitedUsers.getFirst();
      Optional<Chat> existingChat = findExistingDirectChat(currentUser, invitedUser);
      if (existingChat.isPresent()) {
        Chat chat = existingChat.get();
        reactivateMembership(chat, currentUser);
        reactivateMembership(chat, invitedUser);
        return toChatResponse(reloadChatDetails(chat.getChatId()), currentUser);
      }
    }

    if (groupChat && chatName == null) {
      throw new InvalidRequestException("Group chats need a name");
    }

    if (groupChat && !GROUP_NAME_PATTERN.matcher(chatName).matches()) {
      throw new InvalidRequestException(
          "Group name may only contain letters, numbers, dots and underscores");
    }

    if (groupChat && invitedUsers.size() + 1 > MAX_GROUP_MEMBERS) {
      throw new InvalidRequestException("Group chats can have at most 50 members");
    }

    Chat chat =
        Chat.builder()
            .chatId(UUID.randomUUID())
            .name(chatName)
            .createdAt(LocalDateTime.now())
            .owner(groupChat ? userRepository.getReferenceById(currentUser.getId()) : null)
            .members(new ArrayList<>())
            .build();

    List<User> members = new ArrayList<>();
    members.add(currentUser);
    members.addAll(invitedUsers);

    LocalDateTime now = LocalDateTime.now();
    for (User member : members) {
      chat.getMembers()
          .add(
              ChatMember.builder()
                  .id(new ChatMemberId(chat.getChatId(), member.getId()))
                  .chat(chat)
                  .user(member)
                  .joinedAt(now)
                  .lastReadAt(now)
                  .build());
    }

    return toChatResponse(chatRepository.save(chat), currentUser);
  }

  @Transactional
  public List<ChatMessageResponse> getMessages(User currentUser, UUID chatId) {
    return getMessages(currentUser, chatId, null, DEFAULT_MESSAGE_PAGE_SIZE);
  }

  @Transactional
  public List<ChatMessageResponse> getMessages(
      User currentUser, UUID chatId, String before, Integer limit) {
    Chat chat = requireChatMember(currentUser, chatId);
    LocalDateTime beforeCursor = parseMessageCursor(before);
    PageRequest page = PageRequest.of(0, normalizeMessagePageSize(limit));
    List<UUID> messageIds =
        beforeCursor == null
            ? messageRepository.findLatestMessageIds(chat, page)
            : messageRepository.findMessageIdsBefore(chat, beforeCursor, page);

    List<ChatMessageResponse> messages =
        loadMessagesByIds(messageIds).stream()
            .sorted(
                Comparator.comparing(
                    Message::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(message -> toMessageResponse(message, currentUser))
            .toList();
    markChatAsRead(currentUser, chat);
    return messages;
  }

  @Transactional
  public void markChatAsRead(User currentUser, UUID chatId) {
    Chat chat = requireChatMember(currentUser, chatId);
    markChatAsRead(currentUser, chat);
  }

  @Transactional
  public ChatMessageResponse sendMessage(
      User currentUser, UUID chatId, SendMessageRequest request) {
    Chat chat = requireChatMember(currentUser, chatId);
    String content = trimToNull(request.content());
    String postIdToShare = trimToNull(request.postIdToShare());
    if (content == null && postIdToShare == null) {
      throw new InvalidRequestException("Message content or shared post must be present");
    }

    Shared shared = postIdToShare == null ? null : createSharedPost(currentUser, postIdToShare);
    Message message =
        Message.builder()
            .messageId(UUID.randomUUID())
            .chat(chat)
            .fromUser(currentUser)
            .content(content == null ? "" : content)
            .date(LocalDateTime.now())
            .shared(shared)
            .build();

    Message savedMessage = messageRepository.save(message);
    ChatMessageResponse response = toMessageResponse(savedMessage, currentUser);
    broadcastChatEvents(chat, savedMessage, "MESSAGE");
    return response;
  }

  @Transactional
  public ChatResponse uploadGroupImage(User currentUser, UUID chatId, MultipartFile file) {
    Chat chat = requireChatMember(currentUser, chatId);
    if (!isGroupChat(chat)) {
      throw new InvalidRequestException("Only group chats can have a group image");
    }

    User owner = resolveGroupOwner(chat);
    if (owner == null || !owner.getId().equals(currentUser.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the group owner can change the group image");
    }

    String previousImageRef = chat.getImageRef();
    String imageRef = fileStorageService.uploadGroupImage(file);
    chat.setImageRef(imageRef);
    Chat savedChat = chatRepository.save(chat);
    fileStorageService.deleteGroupImage(previousImageRef);

    return toChatResponse(savedChat, currentUser);
  }

  @Transactional
  public void deleteChat(User currentUser, UUID chatId) {
    Chat chat = requireChatMember(currentUser, chatId);
    if (!isGroupChat(chat)) {
      throw new InvalidRequestException("Only group chats can be deleted");
    }

    if (chat.getOwner() == null || !chat.getOwner().getId().equals(currentUser.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can delete it");
    }

    List<String> activeMemberUsernames =
        activeMembers(chat).stream().map(member -> member.getUser().getUsername()).toList();
    chatRepository.delete(chat);
    activeMemberUsernames.forEach(
        username -> chatMessageBroadcaster.publishChatRemoved(username, chatId));
  }

  @Transactional
  public ChatResponse addMembers(User currentUser, UUID chatId, AddChatMembersRequest request) {
    Chat chat = requireGroupOwner(currentUser, chatId);

    List<User> invitedUsers = resolveInvitedUsers(currentUser, request.usernames());
    Set<UUID> existingMemberIds =
        chat.getMembers().stream()
            .map(member -> member.getUser().getId())
            .collect(Collectors.toSet());

    List<User> newMembers =
        invitedUsers.stream().filter(user -> !existingMemberIds.contains(user.getId())).toList();

    if (newMembers.isEmpty()) {
      throw new InvalidRequestException("All selected users are already members");
    }

    int activeMemberCount = activeMembers(chat).size();
    if (activeMemberCount + newMembers.size() > MAX_GROUP_MEMBERS) {
      throw new InvalidRequestException("Group chats can have at most 50 members");
    }

    LocalDateTime now = LocalDateTime.now();
    for (User member : newMembers) {
      chat.getMembers()
          .add(
              ChatMember.builder()
                  .id(new ChatMemberId(chat.getChatId(), member.getId()))
                  .chat(chat)
                  .user(member)
                  .joinedAt(now)
                  .lastReadAt(now)
                  .build());
    }

    chatRepository.save(chat);
    createGroupSystemMessage(chat, currentUser, SystemEventType.MEMBER_ADDED, newMembers);

    return toChatResponse(reloadChatDetails(chatId), currentUser);
  }

  @Transactional
  public ChatMessageResponse reactToMessage(
      User currentUser, UUID chatId, UUID messageId, AddReactionRequest request) {
    Chat chat = requireChatMember(currentUser, chatId);
    String emoji = validateReactionEmoji(request.emoji());
    Message message = requireMessageInChat(chat, messageId);
    if (message.isSystem()) {
      throw new InvalidRequestException("Cannot react to system messages");
    }
    ReactionId reactionId = new ReactionId(currentUser.getId(), messageId);
    Optional<Reaction> existingReaction = reactionRepository.findById(reactionId);

    if (existingReaction.isPresent()) {
      Reaction reaction = existingReaction.get();
      if (emoji.equals(reaction.getContent())) {
        reactionRepository.delete(reaction);
      } else {
        reaction.setContent(emoji);
        reactionRepository.save(reaction);
      }
    } else {
      reactionRepository.save(
          Reaction.builder()
              .id(reactionId)
              .message(message)
              .fromUser(currentUser)
              .content(emoji)
              .createdAt(LocalDateTime.now())
              .build());
    }

    Message updatedMessage = requireMessageWithReactions(messageId);
    ChatMessageResponse response = toMessageResponse(updatedMessage, currentUser);
    broadcastChatEvents(chat, updatedMessage, "MESSAGE_UPDATE");
    return response;
  }

  @Transactional
  public ChatResponse removeMember(User currentUser, UUID chatId, UUID memberId) {
    Chat chat = requireGroupOwner(currentUser, chatId);
    User owner = resolveGroupOwner(chat);
    if (owner == null) {
      throw new InvalidRequestException("Group owner could not be resolved");
    }

    if (memberId.equals(currentUser.getId())) {
      throw new InvalidRequestException("Use leave group to remove yourself");
    }

    if (memberId.equals(owner.getId())) {
      throw new InvalidRequestException("The group owner cannot be removed");
    }

    boolean isMember =
        chat.getMembers().stream().anyMatch(member -> member.getUser().getId().equals(memberId));
    if (!isMember) {
      throw new NotFoundException("Chat member", memberId.toString());
    }

    User removedMember =
        chat.getMembers().stream()
            .map(ChatMember::getUser)
            .filter(user -> user.getId().equals(memberId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Chat member", memberId.toString()));

    int deletedRows = chatMemberRepository.deleteByChatIdAndUserId(chatId, memberId);
    if (deletedRows == 0) {
      throw new NotFoundException("Chat member", memberId.toString());
    }

    chatMessageBroadcaster.publishChatRemoved(removedMember.getUsername(), chatId);
    createGroupSystemMessage(
        chat, currentUser, SystemEventType.MEMBER_REMOVED, List.of(removedMember));

    return toChatResponse(reloadChatDetails(chatId), currentUser);
  }

  @Transactional
  public void leaveChat(User currentUser, UUID chatId) {
    Chat chat = requireChatMember(currentUser, chatId);
    if (isGroupChat(chat)) {
      User owner = resolveGroupOwner(chat);
      if (owner != null && owner.getId().equals(currentUser.getId())) {
        throw new InvalidRequestException("Group owners must delete the group instead of leaving");
      }

      chatMemberRepository.deleteByChatIdAndUserId(chatId, currentUser.getId());
      createGroupSystemMessage(chat, currentUser, SystemEventType.MEMBER_LEFT, List.of());
      chatMessageBroadcaster.publishChatRemoved(currentUser.getUsername(), chatId);
      return;
    }

    markMemberLeft(chatId, currentUser.getId());
    chatMessageBroadcaster.publishChatRemoved(currentUser.getUsername(), chatId);
  }

  private List<User> resolveInvitedUsers(User currentUser, List<String> usernames) {
    Set<String> cleanedUsernames = new LinkedHashSet<>();
    for (String username : usernames) {
      String cleanedUsername = trimToNull(username);
      if (cleanedUsername != null) {
        cleanedUsernames.add(cleanedUsername);
      }
    }

    if (cleanedUsernames.isEmpty()) {
      throw new InvalidRequestException("At least one friend must be selected");
    }

    List<User> invitedUsers = new ArrayList<>();
    for (String username : cleanedUsernames) {
      if (currentUser.getUsername().equals(username)) {
        throw new InvalidRequestException("Cannot create a chat with yourself");
      }

      User invitedUser =
          userRepository
              .findByUsername(username)
              .orElseThrow(() -> new NotFoundException("User", username));

      if (!friendRelationRepository.existsByUsers(currentUser, invitedUser)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chats can only include friends");
      }

      invitedUsers.add(invitedUser);
    }

    return invitedUsers;
  }

  private Optional<Chat> findExistingDirectChat(User currentUser, User invitedUser) {
    return chatRepository
        .findDirectChatsBetweenUsers(currentUser.getId(), invitedUser.getId())
        .stream()
        .filter(chat -> !isGroupChat(chat))
        .max(Comparator.comparing(this::latestActivityAt));
  }

  private List<Message> loadMessagesByIds(List<UUID> messageIds) {
    if (messageIds.isEmpty()) {
      return List.of();
    }

    return messageRepository.findMessagesWithDetailsByIds(messageIds);
  }

  private int normalizeMessagePageSize(Integer limit) {
    if (limit == null) {
      return DEFAULT_MESSAGE_PAGE_SIZE;
    }

    return Math.min(Math.max(limit, 1), MAX_MESSAGE_PAGE_SIZE);
  }

  private LocalDateTime parseMessageCursor(String before) {
    String trimmedBefore = trimToNull(before);
    if (trimmedBefore == null) {
      return null;
    }

    try {
      return OffsetDateTime.parse(trimmedBefore).toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      try {
        return LocalDateTime.parse(trimmedBefore);
      } catch (DateTimeParseException ex) {
        throw new InvalidRequestException("Invalid message pagination cursor");
      }
    }
  }

  private List<Chat> loadChatsForUser(User currentUser) {
    return chatRepository.findActiveChatsForUserWithDetails(currentUser.getId());
  }

  private Chat reloadChatDetails(UUID chatId) {
    entityManager.flush();
    entityManager.clear();
    return chatRepository
        .findByChatIdWithDetails(chatId)
        .orElseThrow(() -> new NotFoundException("Chat", chatId.toString()));
  }

  private LocalDateTime latestActivityAt(Chat chat) {
    return messageRepository
        .findTopByChatOrderByDateDesc(chat)
        .map(Message::getDate)
        .orElse(chat.getCreatedAt());
  }

  private List<ChatResponse> dedupeDirectChats(List<ChatResponse> chats, User currentUser) {
    Map<String, ChatResponse> directByOtherUser = new LinkedHashMap<>();
    List<ChatResponse> result = new ArrayList<>();

    for (ChatResponse chat : chats) {
      if (chat.groupChat()) {
        result.add(chat);
        continue;
      }

      String otherUserKey = resolveDirectChatPartnerKey(chat, currentUser);

      directByOtherUser.merge(
          otherUserKey,
          chat,
          (existing, incoming) ->
              chatActivityAt(incoming).isAfter(chatActivityAt(existing)) ? incoming : existing);
    }

    result.addAll(directByOtherUser.values());
    result.sort(
        Comparator.comparing(
            this::chatActivityAt, Comparator.nullsLast(Comparator.reverseOrder())));
    return result;
  }

  private LocalDateTime chatActivityAt(ChatResponse chat) {
    if (chat.lastMessage() != null) {
      return chat.lastMessage().date();
    }
    return chat.createdAt();
  }

  private Chat requireGroupOwner(User currentUser, UUID chatId) {
    Chat chat = requireChatMember(currentUser, chatId);
    if (!isGroupChat(chat)) {
      throw new InvalidRequestException("This action is only available for group chats");
    }

    User owner = resolveGroupOwner(chat);
    if (owner == null || !owner.getId().equals(currentUser.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only the group owner can manage members");
    }

    return chat;
  }

  private void broadcastChatEvents(Chat chat, Message message, String eventType) {
    Chat chatWithMembers =
        chatRepository
            .findByChatIdWithDetails(chat.getChatId())
            .orElseThrow(() -> new NotFoundException("Chat", chat.getChatId().toString()));
    List<ChatMember> activeMembers = activeMembers(chatWithMembers);
    Map<UUID, Long> personalMessageCounts =
        countPersonalMessagesByUser(chatWithMembers, activeMembers);
    Map<UUID, Long> unreadMessageCounts = countUnreadMessagesByUser(chatWithMembers, activeMembers);
    Message summaryLastMessage =
        "MESSAGE_UPDATE".equals(eventType)
            ? messageRepository.findTopByChatOrderByDateDesc(chatWithMembers).orElse(null)
            : message;

    for (ChatMember member : activeMembers) {
      UUID memberId = member.getUser().getId();
      ChatMessageResponse messageForMember = toMessageResponse(message, member.getUser());
      ChatMessageResponse lastMessageForMember =
          summaryLastMessage == null
              ? null
              : toMessageResponse(summaryLastMessage, member.getUser());
      chatMessageBroadcaster.publishMessageEvent(
          member.getUser().getUsername(),
          chat.getChatId(),
          new ChatRealtimePayload(eventType, chat.getChatId(), messageForMember));
      ChatResponse summary =
          toChatResponse(
              chatWithMembers,
              member.getUser(),
              lastMessageForMember,
              personalMessageCounts.getOrDefault(memberId, 0L),
              unreadMessageCounts.getOrDefault(memberId, 0L));
      chatMessageBroadcaster.publishChatSummary(member.getUser().getUsername(), summary);
    }
  }

  private List<Chat> mergeChatsById(List<Chat> chats) {
    Map<UUID, Chat> merged = new LinkedHashMap<>();
    for (Chat chat : chats) {
      merged.merge(
          chat.getChatId(),
          chat,
          (existing, incoming) -> {
            for (ChatMember member : incoming.getMembers()) {
              boolean alreadyPresent =
                  existing.getMembers().stream()
                      .anyMatch(
                          existingMember ->
                              existingMember.getUser().getId().equals(member.getUser().getId()));
              if (!alreadyPresent) {
                existing.getMembers().add(member);
              }
            }
            if (existing.getOwner() == null && incoming.getOwner() != null) {
              existing.setOwner(incoming.getOwner());
            }
            return existing;
          });
    }
    return new ArrayList<>(merged.values());
  }

  private Chat requireChatMember(User currentUser, UUID chatId) {
    Chat chat =
        chatRepository
            .findByChatIdWithDetails(chatId)
            .orElseThrow(() -> new NotFoundException("Chat", chatId.toString()));

    if (!chatMemberRepository.existsActiveByChatChatIdAndUser(chatId, currentUser)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this chat");
    }

    return chat;
  }

  private ChatResponse toChatResponse(Chat chat, User currentUser) {
    ChatMessageResponse lastMessage =
        messageRepository
            .findTopByChatOrderByDateDesc(chat)
            .map(message -> toMessageResponse(message, currentUser))
            .orElse(null);

    return toChatResponse(
        chat,
        currentUser,
        lastMessage,
        countPersonalMessages(chat, currentUser),
        countUnreadMessages(chat, currentUser));
  }

  private ChatResponse toChatResponse(
      Chat chat,
      User currentUser,
      ChatMessageResponse lastMessage,
      long personalMessageCount,
      long unreadCount) {

    User groupOwner = resolveGroupOwner(chat);
    ChatParticipantResponse groupOwnerResponse =
        groupOwner != null ? toParticipantResponse(groupOwner) : null;

    return new ChatResponse(
        chat.getChatId(),
        isGroupChat(chat)
            ? resolveChatName(chat, currentUser)
            : resolveDirectChatDisplayName(chat, currentUser),
        chat.getImageRef(),
        participantsForResponse(chat, currentUser),
        lastMessage,
        chat.getCreatedAt(),
        isGroupChat(chat),
        groupOwner != null ? groupOwner.getId() : null,
        groupOwnerResponse,
        groupOwner != null && groupOwner.getId().equals(currentUser.getId()),
        personalMessageCount,
        unreadCount);
  }

  private Map<UUID, Long> countPersonalMessagesByUser(Chat chat, List<ChatMember> members) {
    Set<UUID> userIds = activeMemberIds(members);
    if (userIds.isEmpty()) {
      return Map.of();
    }

    return messageRepository.countPersonalMessagesByUsers(chat, userIds).stream()
        .collect(
            Collectors.toMap(
                MessageRepository.UserMessageCount::getUserId,
                MessageRepository.UserMessageCount::getMessageCount));
  }

  private Map<UUID, Long> countUnreadMessagesByUser(Chat chat, List<ChatMember> members) {
    Set<UUID> userIds = activeMemberIds(members);
    if (userIds.isEmpty()) {
      return Map.of();
    }

    return messageRepository.countUnreadMessagesByUsers(chat, userIds).stream()
        .collect(
            Collectors.toMap(
                MessageRepository.UserMessageCount::getUserId,
                MessageRepository.UserMessageCount::getMessageCount));
  }

  private Set<UUID> activeMemberIds(List<ChatMember> members) {
    return members.stream().map(member -> member.getUser().getId()).collect(Collectors.toSet());
  }

  private long countPersonalMessages(Chat chat, User currentUser) {
    return messageRepository.countPersonalMessages(chat, currentUser);
  }

  private long countUnreadMessages(Chat chat, User currentUser) {
    return chat.getMembers().stream()
        .filter(member -> member.getUser().getId().equals(currentUser.getId()))
        .findFirst()
        .map(
            member -> {
              LocalDateTime lastReadAt = member.getLastReadAt();
              if (lastReadAt == null) {
                return messageRepository.countUnreadMessagesWhenNeverRead(chat, currentUser);
              }
              return messageRepository.countUnreadMessagesSince(chat, currentUser, lastReadAt);
            })
        .orElse(0L);
  }

  private void markChatAsRead(User currentUser, Chat chat) {
    chatMemberRepository
        .findByChatChatIdAndUser(chat.getChatId(), currentUser)
        .ifPresent(
            member -> {
              member.setLastReadAt(LocalDateTime.now());
              chatMemberRepository.save(member);
            });
  }

  private User resolveGroupOwner(Chat chat) {
    if (chat.getOwner() != null) {
      return chat.getOwner();
    }

    if (!isGroupChat(chat)) {
      return null;
    }

    return chat.getMembers().stream()
        .min(
            Comparator.comparing(
                ChatMember::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(ChatMember::getUser)
        .orElse(null);
  }

  private ChatMessageResponse toMessageResponse(Message message, User currentUser) {
    List<ChatParticipantResponse> systemTargets = resolveSystemTargets(message);
    String displayContent =
        message.isSystem()
            ? formatSystemMessage(message, currentUser, systemTargets)
            : message.getContent();

    return new ChatMessageResponse(
        message.getMessageId(),
        message.getChat().getChatId(),
        toParticipantResponse(message.getFromUser()),
        displayContent,
        message.getDate(),
        message.getFromUser().getId().equals(currentUser.getId()),
        message.isSystem(),
        message.getSystemEvent() != null ? message.getSystemEvent().name() : null,
        systemTargets,
        toReactionResponses(message, currentUser),
        toSharedPostResponse(message.getShared(), currentUser));
  }

  private Shared createSharedPost(User currentUser, String postId) {
    Post post =
        postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post", postId));
    if (!canViewPost(post, currentUser)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot share this post");
    }

    return sharedRepository.save(
        Shared.builder()
            .sharedId(UUID.randomUUID())
            .post(post)
            .sharedBy(currentUser)
            .createdAt(LocalDateTime.now())
            .build());
  }

  private SharedPostResponse toSharedPostResponse(Shared shared, User currentUser) {
    if (shared == null) {
      return null;
    }

    Post post = shared.getPost();
    boolean externalPost = post.isExternalPost();
    String username =
        externalPost ? post.getExternalAuthor().getDisplayName() : post.getUser().getUsername();
    UUID profileUserId = externalPost ? null : post.getUser().getId();
    String profilePicture = externalPost ? null : post.getUser().getProfilePicture();
    String profileUsername = externalPost ? null : post.getUser().getUsername();
    boolean publicPost = externalPost || post.isPublic();
    boolean removed = Boolean.TRUE.equals(post.getIsRemoved());
    boolean canView = canViewPost(post, currentUser);
    String mediaRef = canView ? post.getMediaRef() : null;
    String unavailableReason = removed ? "REMOVED" : canView ? null : "PRIVATE";

    return new SharedPostResponse(
        shared.getSharedId(),
        post.getPostId(),
        username,
        profileUserId,
        profilePicture,
        canView ? post.getCaption() : null,
        mediaRef,
        canView ? mediaKind(mediaRef) : null,
        shared.getCreatedAt(),
        canView,
        publicPost,
        profileUsername,
        unavailableReason);
  }

  private boolean canViewPost(Post post, User currentUser) {
    if (Boolean.TRUE.equals(post.getIsRemoved())) {
      return false;
    }

    if (post.isExternalPost() || post.isPublic()) {
      return true;
    }

    User owner = post.getUser();
    if (owner == null || currentUser == null) {
      return false;
    }

    return owner.getId().equals(currentUser.getId())
        || friendRelationRepository.existsByUsers(owner, currentUser);
  }

  private String mediaKind(String mediaRef) {
    if (mediaRef == null || mediaRef.isBlank()) {
      return null;
    }

    return mediaRef.toLowerCase().matches(".*\\.(mp4|webm|ogg|mov)(\\?.*)?$") ? "video" : "image";
  }

  private void createGroupSystemMessage(
      Chat chat, User actor, SystemEventType event, List<User> targets) {
    if (!isGroupChat(chat)) {
      return;
    }

    String targetIds =
        targets.stream().map(User::getId).map(UUID::toString).collect(Collectors.joining(","));

    Message message =
        Message.builder()
            .messageId(UUID.randomUUID())
            .chat(chat)
            .fromUser(actor)
            .content("")
            .date(LocalDateTime.now())
            .system(true)
            .systemEvent(event)
            .systemTargetUserIds(targetIds.isBlank() ? null : targetIds)
            .build();

    Message savedMessage = messageRepository.save(message);
    Chat reloadedChat = reloadChatDetails(chat.getChatId());
    Message reloadedMessage =
        messageRepository
            .findWithDetailsByMessageId(savedMessage.getMessageId())
            .orElseThrow(
                () -> new NotFoundException("Message", savedMessage.getMessageId().toString()));
    broadcastChatEvents(reloadedChat, reloadedMessage, "MESSAGE");
  }

  private List<ChatParticipantResponse> resolveSystemTargets(Message message) {
    if (!message.isSystem() || trimToNull(message.getSystemTargetUserIds()) == null) {
      return List.of();
    }

    return Arrays.stream(message.getSystemTargetUserIds().split(","))
        .map(String::trim)
        .filter(id -> !id.isBlank())
        .map(id -> userRepository.findById(UUID.fromString(id)))
        .flatMap(Optional::stream)
        .map(this::toParticipantResponse)
        .toList();
  }

  private String formatSystemMessage(
      Message message, User currentUser, List<ChatParticipantResponse> targets) {
    if (message.getSystemEvent() == null) {
      return trimToNull(message.getContent()) != null ? message.getContent() : "";
    }

    String actorLabel =
        message.getFromUser().getId().equals(currentUser.getId())
            ? "You"
            : formatDisplayName(message.getFromUser());
    boolean currentUserIsTarget =
        targets.stream().anyMatch(target -> target.id().equals(currentUser.getId()));

    return switch (message.getSystemEvent()) {
      case MEMBER_ADDED -> {
        if (message.getFromUser().getId().equals(currentUser.getId())) {
          yield "You added " + formatTargetList(targets, currentUser);
        }
        if (currentUserIsTarget && targets.size() == 1) {
          yield actorLabel + " added you";
        }
        yield actorLabel + " added " + formatTargetList(targets, currentUser);
      }
      case MEMBER_REMOVED -> {
        String removedName =
            targets.isEmpty() ? "a member" : formatTargetList(targets, currentUser);
        if (message.getFromUser().getId().equals(currentUser.getId())) {
          yield "You removed " + removedName;
        }
        yield actorLabel + " removed " + removedName;
      }
      case MEMBER_LEFT -> {
        if (message.getFromUser().getId().equals(currentUser.getId())) {
          yield "You left the group";
        }
        yield actorLabel + " left the group";
      }
    };
  }

  private String formatTargetList(List<ChatParticipantResponse> targets, User currentUser) {
    if (targets.isEmpty()) {
      return "members";
    }

    List<String> labels =
        targets.stream()
            .map(
                target -> {
                  if (target.id().equals(currentUser.getId())) {
                    return "you";
                  }
                  return formatDisplayName(target);
                })
            .toList();

    if (labels.size() == 1) {
      return labels.getFirst();
    }
    if (labels.size() == 2) {
      return labels.getFirst() + " and " + labels.get(1);
    }

    return String.join(", ", labels.subList(0, labels.size() - 1)) + " and " + labels.getLast();
  }

  private String formatDisplayName(User user) {
    String name = trimToNull(user.getName());
    return name != null ? name : user.getUsername();
  }

  private String formatDisplayName(ChatParticipantResponse participant) {
    String name = trimToNull(participant.name());
    return name != null ? name : participant.username();
  }

  private List<MessageReactionResponse> toReactionResponses(Message message, User currentUser) {
    if (message.getReactions() == null || message.getReactions().isEmpty()) {
      return List.of();
    }

    Map<String, Long> counts = new LinkedHashMap<>();
    String currentUserReaction = null;
    for (Reaction reaction : message.getReactions()) {
      counts.merge(reaction.getContent(), 1L, Long::sum);
      if (reaction.getFromUser().getId().equals(currentUser.getId())) {
        currentUserReaction = reaction.getContent();
      }
    }

    final String myReaction = currentUserReaction;
    return counts.entrySet().stream()
        .map(
            entry ->
                new MessageReactionResponse(
                    entry.getKey(), entry.getValue(), entry.getKey().equals(myReaction)))
        .sorted(Comparator.comparing(MessageReactionResponse::emoji))
        .toList();
  }

  private Message requireMessageInChat(Chat chat, UUID messageId) {
    Message message =
        messageRepository
            .findById(messageId)
            .orElseThrow(() -> new NotFoundException("Message", messageId.toString()));

    if (!message.getChat().getChatId().equals(chat.getChatId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Message does not belong to this chat");
    }

    return message;
  }

  private Message requireMessageWithReactions(UUID messageId) {
    return messageRepository
        .findWithDetailsByMessageId(messageId)
        .orElseThrow(() -> new NotFoundException("Message", messageId.toString()));
  }

  private String validateReactionEmoji(String emoji) {
    String cleanedEmoji = trimToNull(emoji);
    if (cleanedEmoji == null || !ALLOWED_REACTION_EMOJIS.contains(cleanedEmoji)) {
      throw new InvalidRequestException("Unsupported reaction emoji");
    }
    return cleanedEmoji;
  }

  private ChatParticipantResponse toParticipantResponse(User user) {
    return new ChatParticipantResponse(
        user.getId(), user.getUsername(), user.getName(), user.getProfilePicture());
  }

  private boolean hasMember(Chat chat, User user) {
    return activeMembers(chat).stream()
        .anyMatch(member -> member.getUser().getId().equals(user.getId()));
  }

  private List<ChatMember> activeMembers(Chat chat) {
    return chat.getMembers().stream().filter(member -> member.getLeftAt() == null).toList();
  }

  private List<ChatParticipantResponse> participantsForResponse(Chat chat, User currentUser) {
    if (isGroupChat(chat)) {
      return activeMembers(chat).stream()
          .map(ChatMember::getUser)
          .map(this::toParticipantResponse)
          .toList();
    }

    return chat.getMembers().stream()
        .filter(
            member ->
                member.getLeftAt() == null || !member.getUser().getId().equals(currentUser.getId()))
        .map(ChatMember::getUser)
        .map(this::toParticipantResponse)
        .toList();
  }

  private boolean hasVisibleDirectPartner(ChatResponse chat, User currentUser) {
    return chat.participants().stream()
        .anyMatch(participant -> !participant.id().equals(currentUser.getId()));
  }

  private void reactivateMembership(Chat chat, User user) {
    ChatMemberId memberId = new ChatMemberId(chat.getChatId(), user.getId());
    LocalDateTime now = LocalDateTime.now();
    Optional<ChatMember> existingMember = chatMemberRepository.findById(memberId);

    if (existingMember.isPresent()) {
      ChatMember member = existingMember.get();
      if (member.getLeftAt() != null) {
        member.setLeftAt(null);
        member.setJoinedAt(now);
        member.setLastReadAt(now);
        chatMemberRepository.save(member);
      }
      return;
    }

    ChatMember member =
        ChatMember.builder()
            .id(memberId)
            .chat(chat)
            .user(user)
            .joinedAt(now)
            .lastReadAt(now)
            .build();
    chat.getMembers().add(member);
    chatMemberRepository.save(member);
  }

  private void markMemberLeft(UUID chatId, UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new NotFoundException("User", userId.toString()));

    chatMemberRepository
        .findByChatChatIdAndUser(chatId, user)
        .ifPresent(
            member -> {
              member.setLeftAt(LocalDateTime.now());
              chatMemberRepository.save(member);
            });
  }

  private String resolveChatName(Chat chat, User currentUser) {
    String explicitName = trimToNull(chat.getName());
    if (explicitName != null) {
      return explicitName;
    }

    return chat.getMembers().stream()
        .map(ChatMember::getUser)
        .filter(user -> !user.getId().equals(currentUser.getId()))
        .map(User::getUsername)
        .reduce((first, second) -> first + ", " + second)
        .orElse("Chat");
  }

  private String resolveDirectChatDisplayName(Chat chat, User currentUser) {
    return chat.getMembers().stream()
        .map(ChatMember::getUser)
        .filter(user -> !user.getId().equals(currentUser.getId()))
        .map(User::getUsername)
        .findFirst()
        .orElse("Chat");
  }

  private String resolveDirectChatPartnerKey(ChatResponse chat, User currentUser) {
    Optional<String> partnerUsername =
        chat.participants().stream()
            .filter(participant -> !participant.id().equals(currentUser.getId()))
            .map(ChatParticipantResponse::username)
            .findFirst();

    if (partnerUsername.isPresent()) {
      return partnerUsername.get();
    }

    if (chat.lastMessage() != null && !chat.lastMessage().own()) {
      return chat.lastMessage().sender().username();
    }

    return chat.chatId().toString();
  }

  private boolean isGroupChat(Chat chat) {
    return chat.getOwner() != null || chat.getMembers().size() > 2;
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}

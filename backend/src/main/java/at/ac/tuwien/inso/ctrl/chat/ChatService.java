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
import at.ac.tuwien.inso.ctrl.user.service.BlockService;
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
  private static final String DELETED_USER_LABEL = "Deleted user";
  private static final String ACCOUNT_DELETED_GROUP_MESSAGE =
          "A user deleted their account and left the group.";
  private static final String ACCOUNT_DELETED_DIRECT_MESSAGE =
          "This user has deleted their account.";
  private static final String GROUP_OWNERS_PROMOTED_MESSAGE =
          "All remaining members are now group owners.";
  private static final Set<String> ALLOWED_REACTION_EMOJIS =
          Set.of(
                  "👍", "❤️", "😂", "😮", "😢", "😡", "🎉", "🔥", "👏", "🙏", "💯", "✨", "😄", "😁", "🥳",
                  "😍", "🤔", "😎", "😭", "😅", "🙌", "💪", "✅", "👀", "🤯", "😬", "🤝", "😴", "🤮", "🤩",
                  "😇", "😱", "🤗", "😤", "🫶", "💀");

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
  private final BlockService blockService;

  @PersistenceContext private EntityManager entityManager;

  @Transactional(readOnly = true)
  public ChatResponse getChat(User currentUser, UUID chatId) {
    return toChatResponse(requireChatMember(currentUser, chatId), currentUser);
  }

  @Transactional(readOnly = true)
  public List<ChatResponse> getChats(User currentUser) {
    List<ChatResponse> chats =
            loadChatsForUser(currentUser).stream()
                    .filter(chat -> isGroupChat(chat) || !isDirectChatBlockedForUser(chat, currentUser))
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
      if (blockService.inBlockedRelationship(currentUser, invitedUser)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This chat is not available");
      }
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

  @Transactional(readOnly = true)
  public void publishBlockedDirectChatRemoval(User currentUser, String blockedUsername) {
    User blockedUser =
            userRepository
                    .findByUsername(blockedUsername)
                    .orElseThrow(() -> new NotFoundException("User", blockedUsername));

    chatRepository.findDirectChatsBetweenUsers(currentUser.getId(), blockedUser.getId()).stream()
            .filter(chat -> !isGroupChat(chat))
            .map(Chat::getChatId)
            .distinct()
            .forEach(
                    chatId -> {
                      chatMessageBroadcaster.publishChatRemoved(currentUser.getUsername(), chatId);
                      chatMessageBroadcaster.publishChatRemoved(blockedUser.getUsername(), chatId);
                    });
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
    if (isDirectChatWithDeletedParticipant(chat)) {
      throw new InvalidRequestException("You cannot send messages to a deleted account");
    }

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

    if (!isEffectiveGroupOwner(chat, currentUser)) {
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

    if (!isEffectiveGroupOwner(chat, currentUser)) {
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

    int activeMemberCount = activeNonDeletedMembers(chat).size();
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
    List<Reaction> currentUserReactions =
            reactionRepository.findAllByUserIdAndMessageId(currentUser.getId(), messageId);
    boolean sameEmojiAlreadyActive =
            currentUserReactions.stream().anyMatch(reaction -> emoji.equals(reaction.getContent()));

    if (!currentUserReactions.isEmpty()) {
      reactionRepository.deleteAllByUserIdAndMessageId(currentUser.getId(), messageId);
    }

    if (!sameEmojiAlreadyActive) {
      reactionRepository.save(
              Reaction.builder()
                      .id(new ReactionId(currentUser.getId(), messageId, emoji))
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

    if (memberId.equals(currentUser.getId())) {
      throw new InvalidRequestException("Use leave group to remove yourself");
    }

    if (hasActiveStoredGroupOwner(chat) && isEffectiveGroupOwner(chat, memberId)) {
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
      if (isActiveStoredGroupOwner(chat, currentUser)) {
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

  @Transactional
  public void createAccountDeletedSystemMessages(User deletedUser) {
    UUID deletedUserId = deletedUser.getId();
    List<AccountDeletionGroupUpdate> affectedGroups =
            mergeChatsById(chatRepository.findActiveChatsForUserWithDetails(deletedUserId)).stream()
                    .filter(this::isGroupChat)
                    .map(chat -> toAccountDeletionGroupUpdate(chat, deletedUserId))
                    .toList();

    for (AccountDeletionGroupUpdate group : affectedGroups) {
      markMemberLeft(group.chatId(), deletedUserId);
      createPlainSystemMessage(group.chatId(), deletedUserId, ACCOUNT_DELETED_GROUP_MESSAGE);
      if (group.promoteRemainingMembers()) {
        createPlainSystemMessage(group.chatId(), deletedUserId, GROUP_OWNERS_PROMOTED_MESSAGE);
      }
    }

    mergeChatsById(chatRepository.findActiveChatsForUserWithDetails(deletedUserId)).stream()
            .filter(chat -> !isGroupChat(chat))
            .forEach(chat -> handleAccountDeletedDirectChat(chat, deletedUser));
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
    return mergeChatsById(chatRepository.findActiveChatsForUserWithDetails(currentUser.getId()));
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
    Map<String, ChatResponse> groupByMembers = new LinkedHashMap<>();
    Map<String, ChatResponse> directByOtherUser = new LinkedHashMap<>();

    for (ChatResponse chat : chats) {
      if (chat.groupChat()) {
        groupByMembers.merge(
                chat.chatId().toString(),
                chat,
                (existing, incoming) ->
                        chatActivityAt(incoming).isAfter(chatActivityAt(existing)) ? incoming : existing);
        continue;
      }

      String otherUserKey = resolveDirectChatPartnerKey(chat, currentUser);

      directByOtherUser.merge(
              otherUserKey,
              chat,
              (existing, incoming) ->
                      chatActivityAt(incoming).isAfter(chatActivityAt(existing)) ? incoming : existing);
    }

    List<ChatResponse> result = new ArrayList<>();
    result.addAll(groupByMembers.values());
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

    if (!isEffectiveGroupOwner(chat, currentUser)) {
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
    if (!isGroupChat(chatWithMembers)) {
      activeMembers =
              activeMembers.stream()
                      .filter(member -> !isDirectChatBlockedForUser(chatWithMembers, member.getUser()))
                      .toList();
    }
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

    ensureDirectChatNotBlocked(currentUser, chat);
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

    List<User> groupOwners = resolveGroupOwners(chat);
    User groupOwner = groupOwners.isEmpty() ? null : groupOwners.getFirst();
    ChatParticipantResponse groupOwnerResponse =
            groupOwner != null ? toParticipantResponse(groupOwner) : null;
    List<UUID> ownerIds = groupOwners.stream().map(User::getId).toList();

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
            ownerIds,
            groupOwnerResponse,
            ownerIds.contains(currentUser.getId()),
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
    List<User> groupOwners = resolveGroupOwners(chat);
    return groupOwners.isEmpty() ? null : groupOwners.getFirst();
  }

  private List<User> resolveGroupOwners(Chat chat) {
    if (!isGroupChat(chat)) {
      return List.of();
    }

    if (chat.getOwner() != null && !chat.getOwner().isDeleted()) {
      return List.of(chat.getOwner());
    }

    List<User> owners =
            activeMembers(chat).stream()
                    .sorted(
                            Comparator.comparing(
                                    ChatMember::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(ChatMember::getUser)
                    .filter(user -> !user.isDeleted())
                    .toList();

    return distinctUsersById(owners);
  }

  private List<User> distinctUsersById(List<User> users) {
    Map<UUID, User> usersById = new LinkedHashMap<>();
    for (User user : users) {
      usersById.putIfAbsent(user.getId(), user);
    }
    return new ArrayList<>(usersById.values());
  }

  private List<ChatParticipantResponse> distinctParticipantsById(
          List<ChatParticipantResponse> participants) {
    Map<UUID, ChatParticipantResponse> participantsById = new LinkedHashMap<>();
    for (ChatParticipantResponse participant : participants) {
      participantsById.putIfAbsent(participant.id(), participant);
    }
    return new ArrayList<>(participantsById.values());
  }

  private List<ChatMember> activeNonDeletedMembers(Chat chat) {
    return activeMembers(chat).stream().filter(member -> !member.getUser().isDeleted()).toList();
  }

  private boolean isEffectiveGroupOwner(Chat chat, User user) {
    return isEffectiveGroupOwner(chat, user.getId());
  }

  private boolean isEffectiveGroupOwner(Chat chat, UUID userId) {
    return resolveGroupOwners(chat).stream().anyMatch(owner -> owner.getId().equals(userId));
  }

  private boolean hasActiveStoredGroupOwner(Chat chat) {
    return chat.getOwner() != null && !chat.getOwner().isDeleted();
  }

  private boolean isActiveStoredGroupOwner(Chat chat, User user) {
    return hasActiveStoredGroupOwner(chat) && chat.getOwner().getId().equals(user.getId());
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
    if (post == null) {
      // Post was hard-deleted from the database (e.g. by an admin).
      // Return an unavailable response so the chat still loads cleanly.
      return new SharedPostResponse(
              shared.getSharedId(),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              shared.getCreatedAt(),
              false,
              false,
              null,
              "REMOVED");
    }

    boolean externalPost = post.isExternalPost();
    User postOwner = post.getUser();
    boolean deletedOwner = postOwner != null && postOwner.isDeleted();
    String username =
            externalPost
                    ? post.getExternalAuthor().getDisplayName()
                    : deletedOwner ? DELETED_USER_LABEL : postOwner.getUsername();
    UUID profileUserId = externalPost || deletedOwner ? null : postOwner.getId();
    String profilePicture = externalPost || deletedOwner ? null : postOwner.getProfilePicture();
    String profileUsername = externalPost || deletedOwner ? null : postOwner.getUsername();
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

  private AccountDeletionGroupUpdate toAccountDeletionGroupUpdate(Chat chat, UUID deletedUserId) {
    boolean storedOwnerDeleted =
            chat.getOwner() != null && chat.getOwner().getId().equals(deletedUserId);
    boolean hasRemainingMembers =
            activeMembers(chat).stream()
                    .map(ChatMember::getUser)
                    .anyMatch(user -> !user.getId().equals(deletedUserId) && !user.isDeleted());
    return new AccountDeletionGroupUpdate(
            chat.getChatId(), storedOwnerDeleted && hasRemainingMembers);
  }

  private void createPlainSystemMessage(UUID chatId, UUID actorId, String content) {
    Chat chat =
            chatRepository
                    .findByChatIdWithDetails(chatId)
                    .orElseThrow(() -> new NotFoundException("Chat", chatId.toString()));

    Message message =
            Message.builder()
                    .messageId(UUID.randomUUID())
                    .chat(chat)
                    .fromUser(userRepository.getReferenceById(actorId))
                    .content(content)
                    .date(LocalDateTime.now())
                    .system(true)
                    .build();

    Message savedMessage = messageRepository.save(message);
    Chat reloadedChat = reloadChatDetails(chatId);
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
    if (user.isDeleted()) {
      return DELETED_USER_LABEL;
    }

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
    Map<String, List<String>> reactionUsers = new LinkedHashMap<>();
    Set<String> currentUserReactions = new LinkedHashSet<>();
    for (Reaction reaction : message.getReactions()) {
      counts.merge(reaction.getContent(), 1L, Long::sum);
      reactionUsers
              .computeIfAbsent(reaction.getContent(), ignored -> new ArrayList<>())
              .add(formatDisplayName(reaction.getFromUser()));
      if (reaction.getFromUser().getId().equals(currentUser.getId())) {
        currentUserReactions.add(reaction.getContent());
      }
    }

    return counts.entrySet().stream()
            .map(
                    entry ->
                            new MessageReactionResponse(
                                    entry.getKey(),
                                    entry.getValue(),
                                    currentUserReactions.contains(entry.getKey()),
                                    reactionUsers.getOrDefault(entry.getKey(), List.of()).stream()
                                            .distinct()
                                            .sorted(String::compareToIgnoreCase)
                                            .toList()))
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
    if (user.isDeleted()) {
      return new ChatParticipantResponse(user.getId(), DELETED_USER_LABEL, null, null, true);
    }

    return new ChatParticipantResponse(
            user.getId(), user.getUsername(), user.getName(), user.getProfilePicture(), false);
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
      return distinctParticipantsById(
              activeMembers(chat).stream()
                      .map(ChatMember::getUser)
                      .filter(user -> !user.isDeleted())
                      .map(this::toParticipantResponse)
                      .toList());
    }

    return distinctParticipantsById(
            chat.getMembers().stream()
                    .filter(
                            member ->
                                    member.getLeftAt() == null
                                            || !member.getUser().getId().equals(currentUser.getId()))
                    .map(ChatMember::getUser)
                    .map(this::toParticipantResponse)
                    .toList());
  }

  private boolean hasVisibleDirectPartner(ChatResponse chat, User currentUser) {
    return chat.participants().stream()
            .anyMatch(participant -> !participant.id().equals(currentUser.getId()));
  }

  private void ensureDirectChatNotBlocked(User currentUser, Chat chat) {
    if (isDirectChatBlockedForUser(chat, currentUser)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This chat is not available");
    }
  }

  private boolean isDirectChatBlockedForUser(Chat chat, User currentUser) {
    if (isGroupChat(chat) || currentUser == null) {
      return false;
    }

    return activeMembers(chat).stream()
            .map(ChatMember::getUser)
            .filter(user -> !user.getId().equals(currentUser.getId()))
            .filter(user -> !user.isDeleted())
            .anyMatch(user -> blockService.inBlockedRelationship(currentUser, user));
  }

  private boolean isDirectChatWithDeletedParticipant(Chat chat) {
    return !isGroupChat(chat)
            && chat.getMembers().stream().map(ChatMember::getUser).anyMatch(User::isDeleted);
  }

  private void handleAccountDeletedDirectChat(Chat chat, User deletedUser) {
    List<User> remainingUsers =
            activeMembers(chat).stream()
                    .map(ChatMember::getUser)
                    .filter(user -> !user.getId().equals(deletedUser.getId()))
                    .filter(user -> !user.isDeleted())
                    .toList();

    if (messageRepository.countByChat(chat) == 0) {
      for (User remainingUser : remainingUsers) {
        chatMessageBroadcaster.publishChatRemoved(remainingUser.getUsername(), chat.getChatId());
      }
      chatRepository.delete(chat);
      return;
    }

    markMemberLeft(chat.getChatId(), deletedUser.getId());
    createPlainSystemMessage(chat.getChatId(), deletedUser.getId(), ACCOUNT_DELETED_DIRECT_MESSAGE);
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
    Optional<UUID> partnerId =
            chat.participants().stream()
                    .filter(participant -> !participant.id().equals(currentUser.getId()))
                    .map(ChatParticipantResponse::id)
                    .findFirst();

    if (partnerId.isPresent()) {
      return partnerId.get().toString();
    }

    if (chat.lastMessage() != null && !chat.lastMessage().own()) {
      return chat.lastMessage().sender().id().toString();
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

  private record AccountDeletionGroupUpdate(UUID chatId, boolean promoteRemainingMembers) {}
}
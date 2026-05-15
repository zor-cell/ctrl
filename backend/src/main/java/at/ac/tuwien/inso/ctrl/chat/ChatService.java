package at.ac.tuwien.inso.ctrl.chat;

import at.ac.tuwien.inso.ctrl.chat.dto.ChatMessageResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatParticipantResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.ChatResponse;
import at.ac.tuwien.inso.ctrl.chat.dto.CreateChatRequest;
import at.ac.tuwien.inso.ctrl.chat.dto.SendMessageRequest;
import at.ac.tuwien.inso.ctrl.chat.entity.Chat;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMemberId;
import at.ac.tuwien.inso.ctrl.chat.entity.Message;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatMemberRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.MessageRepository;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.NotFoundException;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatRepository chatRepository;
  private final ChatMemberRepository chatMemberRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final FriendRelationRepository friendRelationRepository;

  @Transactional(readOnly = true)
  public List<ChatResponse> getChats(User currentUser) {
    return chatRepository.findChatsForUser(currentUser).stream()
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
  }

  @Transactional
  public ChatResponse createChat(User currentUser, CreateChatRequest request) {
    List<User> invitedUsers = resolveInvitedUsers(currentUser, request.usernames());
    String chatName = trimToNull(request.name());
    boolean groupChat = invitedUsers.size() > 1 || chatName != null;

    if (!groupChat) {
      Optional<Chat> existingChat = findExistingDirectChat(currentUser, invitedUsers.getFirst());
      if (existingChat.isPresent()) {
        return toChatResponse(existingChat.get(), currentUser);
      }
    }

    if (groupChat && chatName == null) {
      throw new InvalidRequestException("Group chats need a name");
    }

    Chat chat =
        Chat.builder()
            .chatId(UUID.randomUUID())
            .name(chatName)
            .createdAt(LocalDateTime.now())
            .owner(groupChat ? currentUser : null)
            .members(new ArrayList<>())
            .build();

    List<User> members = new ArrayList<>();
    members.add(currentUser);
    members.addAll(invitedUsers);

    for (User member : members) {
      chat.getMembers()
          .add(
              ChatMember.builder()
                  .id(new ChatMemberId(chat.getChatId(), member.getId()))
                  .chat(chat)
                  .user(member)
                  .joinedAt(LocalDateTime.now())
                  .build());
    }

    return toChatResponse(chatRepository.save(chat), currentUser);
  }

  @Transactional(readOnly = true)
  public List<ChatMessageResponse> getMessages(User currentUser, UUID chatId) {
    Chat chat = requireChatMember(currentUser, chatId);
    return messageRepository.findByChatOrderByDateAsc(chat).stream()
        .map(message -> toMessageResponse(message, currentUser))
        .toList();
  }

  @Transactional
  public ChatMessageResponse sendMessage(
      User currentUser, UUID chatId, SendMessageRequest request) {
    Chat chat = requireChatMember(currentUser, chatId);
    String content = trimToNull(request.content());
    if (content == null) {
      throw new InvalidRequestException("Message content must not be blank");
    }

    Message message =
        Message.builder()
            .messageId(UUID.randomUUID())
            .chat(chat)
            .fromUser(currentUser)
            .content(content)
            .date(LocalDateTime.now())
            .build();

    return toMessageResponse(messageRepository.save(message), currentUser);
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

    chatRepository.delete(chat);
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
    return chatRepository.findChatsForUser(currentUser).stream()
        .filter(chat -> trimToNull(chat.getName()) == null)
        .filter(chat -> chat.getMembers().size() == 2)
        .filter(chat -> hasMember(chat, invitedUser))
        .findFirst();
  }

  private Chat requireChatMember(User currentUser, UUID chatId) {
    Chat chat =
        chatRepository
            .findByChatId(chatId)
            .orElseThrow(() -> new NotFoundException("Chat", chatId.toString()));

    if (!chatMemberRepository.existsByChatChatIdAndUser(chatId, currentUser)) {
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

    return new ChatResponse(
        chat.getChatId(),
        resolveChatName(chat, currentUser),
        chat.getMembers().stream()
            .map(ChatMember::getUser)
            .map(this::toParticipantResponse)
            .toList(),
        lastMessage,
        chat.getCreatedAt(),
        isGroupChat(chat),
        chat.getOwner() != null && chat.getOwner().getId().equals(currentUser.getId()));
  }

  private ChatMessageResponse toMessageResponse(Message message, User currentUser) {
    return new ChatMessageResponse(
        message.getMessageId(),
        message.getChat().getChatId(),
        toParticipantResponse(message.getFromUser()),
        message.getContent(),
        message.getDate(),
        message.getFromUser().getId().equals(currentUser.getId()));
  }

  private ChatParticipantResponse toParticipantResponse(User user) {
    return new ChatParticipantResponse(
        user.getId(), user.getUsername(), user.getName(), user.getProfilePicture());
  }

  private boolean hasMember(Chat chat, User user) {
    return chat.getMembers().stream()
        .anyMatch(member -> member.getUser().getId().equals(user.getId()));
  }

  private String resolveChatName(Chat chat, User currentUser) {
    String explicitName = trimToNull(chat.getName());
    if (explicitName != null) {
      return explicitName;
    }

    String generatedName =
        chat.getMembers().stream()
            .map(ChatMember::getUser)
            .filter(user -> !user.getId().equals(currentUser.getId()))
            .map(user -> trimToNull(user.getName()) != null ? user.getName() : user.getUsername())
            .reduce((first, second) -> first + ", " + second)
            .orElse("Chat");

    return generatedName;
  }

  private boolean isGroupChat(Chat chat) {
    return trimToNull(chat.getName()) != null || chat.getMembers().size() > 2;
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}

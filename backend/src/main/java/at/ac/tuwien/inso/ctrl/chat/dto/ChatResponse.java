package at.ac.tuwien.inso.ctrl.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChatResponse(
    UUID chatId,
    String name,
    String groupImage,
    List<ChatParticipantResponse> participants,
    ChatMessageResponse lastMessage,
    LocalDateTime createdAt,
    boolean groupChat,
    UUID ownerId,
    ChatParticipantResponse groupOwner,
    boolean owner,
    long unreadCount) {}

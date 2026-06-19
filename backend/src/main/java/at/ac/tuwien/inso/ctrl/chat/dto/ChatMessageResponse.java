package at.ac.tuwien.inso.ctrl.chat.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
    UUID messageId,
    UUID chatId,
    ChatParticipantResponse sender,
    String content,
    LocalDateTime date,
    boolean own,
    boolean system,
    String systemEvent,
    List<ChatParticipantResponse> systemTargets,
    List<MessageReactionResponse> reactions,
    SharedPostResponse sharedPost) {}

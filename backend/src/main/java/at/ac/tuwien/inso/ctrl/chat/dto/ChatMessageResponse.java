package at.ac.tuwien.inso.ctrl.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
    UUID messageId,
    UUID chatId,
    ChatParticipantResponse sender,
    String content,
    LocalDateTime date,
    boolean own) {}

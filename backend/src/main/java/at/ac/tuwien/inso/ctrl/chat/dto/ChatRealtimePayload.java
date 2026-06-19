package at.ac.tuwien.inso.ctrl.chat.dto;

import java.util.UUID;

public record ChatRealtimePayload(String type, UUID chatId, ChatMessageResponse message) {}

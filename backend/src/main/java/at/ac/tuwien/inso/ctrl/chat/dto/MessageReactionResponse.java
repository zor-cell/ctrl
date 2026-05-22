package at.ac.tuwien.inso.ctrl.chat.dto;

public record MessageReactionResponse(String emoji, long count, boolean reactedByCurrentUser) {}

package at.ac.tuwien.inso.ctrl.chat.dto;

import java.util.List;

public record MessageReactionResponse(
    String emoji, long count, boolean reactedByCurrentUser, List<String> users) {}

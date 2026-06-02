package at.ac.tuwien.inso.ctrl.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SharedPostResponse(
    UUID sharedId,
    String postId,
    String username,
    String caption,
    String mediaRef,
    String mediaKind,
    LocalDateTime sharedAt,
    boolean canView,
    boolean publicPost,
    String profileUsername) {}

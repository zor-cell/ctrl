package at.ac.tuwien.inso.ctrl.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostResponse(
    String postId,
    UUID userId,
    String username,
    String sourceId,
    String caption,
    String mediaRef,
    boolean isPublic,
    List<String> labelIds,
    Integer upvotes,
    Integer downvotes,
    LocalDateTime createdAt) {}

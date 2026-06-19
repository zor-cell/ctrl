package at.ac.tuwien.inso.ctrl.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostResponse(
    String postId,
    UUID userId,
    String username,
    boolean userDeleted,
    String sourceId,
    SourceResponse source,
    String caption,
    String mediaRef,
    boolean isPublic,
    boolean isAiGenerated,
    List<LabelResponse> labelIds,
    List<String> topicIds,
    List<CommentResponse> comments,
    Integer upvotes,
    Integer downvotes,
    LocalDateTime createdAt) {}

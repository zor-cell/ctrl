package at.ac.tuwien.inso.ctrl.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
    String postId,
    String userId,
    String username,
    String sourceId,
    SourceResponse source,
    String caption,
    String mediaRef,
    boolean isPublic,
    boolean isAiGenerated,
    List<String> labelIds,
    List<String> topicIds,
    List<CommentResponse> comments,
    Integer upvotes,
    Integer downvotes,
    LocalDateTime createdAt) {}

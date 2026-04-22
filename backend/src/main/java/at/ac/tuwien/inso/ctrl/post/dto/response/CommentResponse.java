package at.ac.tuwien.inso.ctrl.post.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
    String commentId,
    String username,
    String content,
    LocalDateTime createdAt,
    String parentCommentId) {}

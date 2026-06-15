package at.ac.tuwien.inso.ctrl.post.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
    String commentId,
    String username,
    boolean userDeleted,
    String content,
    LocalDateTime createdAt,
    String parentCommentId,
    boolean wasReportedByUser // true if the current user has reported this comment
    ) {}

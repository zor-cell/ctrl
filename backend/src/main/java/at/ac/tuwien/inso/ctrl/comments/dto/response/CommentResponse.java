package at.ac.tuwien.inso.ctrl.comments.dto.response;

import java.util.List;

public record CommentResponse(
    String commentId,
    java.util.UUID userId,
    String username,
    String userProfilePic,
    String createdAt,
    String content,
    String parentCommentId, // null for top-level comments
    boolean edited, // true when the comment has been updated after creation
    List<CommentResponse> replies // nested replies; empty list for leaf comments
    ) {}

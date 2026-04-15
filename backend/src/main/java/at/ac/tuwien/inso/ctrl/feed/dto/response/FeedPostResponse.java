package at.ac.tuwien.inso.ctrl.feed.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FeedPostResponse(
        String postId,
        // entweder interner User oder externer Autor
        String userId,
        String username,
        String userProfilePic,
        // Post-Daten
        String caption,
        String mediaRef,
        boolean isPublic,
        Integer upvotes,
        Integer downvotes,
        LocalDateTime createdAt,
        // Relations
        List<String> topicIds,
        List<LabelResponse> labelIds,
        List<SourceResponse> sourceIds,
        List<CommentResponse> comments
) {
    public record LabelResponse(String name, String description) {}

    public record SourceResponse(String url, Integer upvotes, Integer downvotes) {}

    public record CommentResponse(
            String commentId,
            String userId,
            String username,
            String userProfilePic,
            String content,
            LocalDateTime createdAt,
            String parentCommentId
    ) {}
}
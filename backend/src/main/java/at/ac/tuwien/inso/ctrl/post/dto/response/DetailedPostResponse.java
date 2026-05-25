package at.ac.tuwien.inso.ctrl.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DetailedPostResponse(
    String postId,
    String userId,
    String username,
    String userProfilePicture,
    String sourceId,
    SourceResponse source,
    String caption,
    String mediaUrl,
    boolean isPublic,
    boolean isAiGenerated,
    List<String> labelIds,
    List<String> topicIds,
    List<CommentResponse> comments,
    Integer upvotes,
    Integer downvotes,
    String userVote,
    LocalDateTime createdAt) {}

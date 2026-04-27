package at.ac.tuwien.inso.ctrl.feed.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FeedPostResponse(
    String postId,
    // either an internal user or an external author
    String userId,
    String username,
    String userProfilePic,
    String caption,
    String mediaRef,
    boolean isPublic,
    Integer upvotes,
    Integer downvotes,
    String userVote,
    LocalDateTime createdAt,
    List<String> topicIds,
    List<LabelResponse> labelIds,
    List<SourceResponse> sourceIds,
    List<CommentResponse> comments) {
  public record LabelResponse(String name, String description) {}

  public record SourceResponse(
      String sourceId,
      String url,
      Integer upvotes,
      Integer downvotes,
      String userVote // "up", "down" or null
      ) {}

  public record CommentResponse(
      String commentId,
      String userId,
      String username,
      String userProfilePic,
      String content,
      LocalDateTime createdAt,
      String parentCommentId) {}
}

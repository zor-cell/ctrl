package at.ac.tuwien.inso.ctrl.feed.dto.response;

import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostExplanation;
import java.time.LocalDateTime;
import java.util.List;

public record FeedPostResponse(
    String postId,
    // either an internal user or an external author
    String userId,
    String username,
    String userProfilePic,
    boolean userDeleted,
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
    List<CommentResponse> comments,
    PostExplanation explanation,
    boolean wasReportedByUser) {

  public record SourceResponse(
      String sourceId,
      String url,
      Integer upvotes,
      Integer downvotes,
      String userVote, // "up", "down" or null,
      boolean wasReportedByUser) {}

  public record CommentResponse(
      String commentId,
      String userId,
      String username,
      String userProfilePic,
      boolean userDeleted,
      String content,
      LocalDateTime createdAt,
      String parentCommentId) {}
}

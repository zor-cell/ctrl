package at.ac.tuwien.inso.ctrl.report.dto;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ReportedPostResponse(
    // Report Info
    String reportId,
    String reportReason,
    String reportDescription,
    String reportedByUsername,
    String reportedUsername,
    ContentTypeEnum contentType,
    ReportStatus status,
    LocalDateTime reportedAt,

    // Post Info
    String postId,
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
    List<LabelResponse> labels,
    List<FeedPostResponse.SourceResponse> sources,
    List<FeedPostResponse.CommentResponse> comments) {}

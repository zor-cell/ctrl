package at.ac.tuwien.inso.ctrl.feed.mapper;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.label.entity.PostLabelType;
import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.RecommendedPost;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedMapper {
  private static final String DELETED_USER_LABEL = "Deleted user";

  public FeedPostResponse toFeedPostResponse(
      RecommendedPost recommendedPost,
      Map<String, String> postVotes,
      Map<String, Boolean> postReportedByUser,
      Map<String, String> sourceVotes,
      Map<String, Boolean> sourceReportedByUser,
      Set<PostLabelType> computedLabels) {

    Post post = recommendedPost.getPost();

    boolean deletedUser = isDeletedInternalAuthor(post);
    String authorId = post.isExternalPost() ? null : post.getUser().getId().toString();
    String username =
        post.isExternalPost()
            ? post.getExternalAuthor().getDisplayName()
            : deletedUser ? DELETED_USER_LABEL : post.getUser().getUsername();
    String profilePic =
        post.isExternalPost()
            ? post.getExternalAuthor().getAvatarUrl()
            : deletedUser ? null : post.getUser().getProfilePicture();

    String postUserVote = postVotes.get(post.getPostId());

    List<LabelResponse> labels =
        computedLabels.stream()
            .map(
                type ->
                    new LabelResponse(
                        type.name(), type.getName(), type.getEmoji(), type.getExplanation()))
            .toList();

    List<FeedPostResponse.SourceResponse> sources =
        post.getSource() != null
            ? post.getSource().getSingleSources().stream()
                .map(
                    s ->
                        new FeedPostResponse.SourceResponse(
                            post.getSource().getSourceId(),
                            s.getUrl(),
                            post.getSource().getUpvotes(),
                            post.getSource().getDownvotes(),
                            sourceVotes.get(post.getSource().getSourceId()),
                            sourceReportedByUser != null
                                && Boolean.TRUE.equals(
                                    sourceReportedByUser.get(post.getSource().getSourceId())),
                            post.getSource().getCreatedAt(),
                            s.getJournal(),
                            s.getAuthor(),
                            s.getTitle(),
                            s.getDate()))
                .toList()
            : List.of();

    List<FeedPostResponse.CommentResponse> comments =
        post.getComments().stream().map(this::toCommentResponse).toList();

    List<String> topicIds =
        post.getPostTopics().stream().map(pt -> pt.getTopic().getName()).toList();

    String userProfileUrl = post.isExternalPost() ? post.getExternalAuthor().getProfileUrl() : null;

    return new FeedPostResponse(
        post.getPostId(),
        authorId,
        username,
        profilePic,
        deletedUser,
        post.getCaption(),
        post.getMediaRef(),
        post.isPublic(),
        post.getUpvotes(),
        post.getDownvotes(),
        postUserVote,
        post.getCreatedAt(),
        topicIds,
        labels,
        sources,
        comments,
        recommendedPost.getExplanation(),
        postReportedByUser != null && Boolean.TRUE.equals(postReportedByUser.get(post.getPostId())),
        userProfileUrl);
  }

  public FeedPostResponse toFeedPostResponse(
      Post post, Map<String, String> postVotes, Map<String, String> sourceVotes) {
    boolean deletedUser = isDeletedInternalAuthor(post);
    String authorId = post.isExternalPost() ? null : post.getUser().getId().toString();
    String username =
        post.isExternalPost()
            ? post.getExternalAuthor().getDisplayName()
            : deletedUser ? DELETED_USER_LABEL : post.getUser().getUsername();
    String profilePic =
        post.isExternalPost()
            ? post.getExternalAuthor().getAvatarUrl()
            : deletedUser ? null : post.getUser().getProfilePicture();

    String postUserVote = postVotes.get(post.getPostId());

    List<LabelResponse> labels = List.of();

    List<FeedPostResponse.SourceResponse> sources =
        post.getSource() != null
            ? post.getSource().getSingleSources().stream()
                .map(
                    s ->
                        new FeedPostResponse.SourceResponse(
                            post.getSource().getSourceId(),
                            s.getUrl(),
                            post.getSource().getUpvotes(),
                            post.getSource().getDownvotes(),
                            sourceVotes.get(post.getSource().getSourceId()),
                            false,
                            post.getSource().getCreatedAt(),
                            s.getJournal(),
                            s.getAuthor(),
                            s.getTitle(),
                            s.getDate()))
                .toList()
            : List.of();

    List<FeedPostResponse.CommentResponse> comments =
        post.getComments().stream().map(this::toCommentResponse).toList();

    List<String> topicIds =
        post.getPostTopics().stream().map(pt -> pt.getTopic().getName()).toList();

    String userProfileUrl = post.isExternalPost() ? post.getExternalAuthor().getProfileUrl() : null;

    return new FeedPostResponse(
        post.getPostId(),
        authorId,
        username,
        profilePic,
        deletedUser,
        post.getCaption(),
        post.getMediaRef(),
        post.isPublic(),
        post.getUpvotes(),
        post.getDownvotes(),
        postUserVote,
        post.getCreatedAt(),
        topicIds,
        labels,
        sources,
        comments,
        null,
        false,
        userProfileUrl);
  }

  public FeedPostResponse.CommentResponse toCommentResponse(Comment comment) {
    boolean deletedUser = comment.getUser().isDeleted();
    return new FeedPostResponse.CommentResponse(
        comment.getCommentId(),
        comment.getUser().getId().toString(),
        deletedUser ? DELETED_USER_LABEL : comment.getUser().getUsername(),
        deletedUser ? null : comment.getUser().getProfilePicture(),
        deletedUser,
        comment.getContent(),
        comment.getCreatedAt(),
        comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null);
  }

  private boolean isDeletedInternalAuthor(Post post) {
    return !post.isExternalPost() && post.getUser().isDeleted();
  }
}

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

  public FeedPostResponse toFeedPostResponse(
      RecommendedPost recommendedPost,
      Map<String, String> postVotes,
      Map<String, Boolean> postReportedByUser,
      Map<String, String> sourceVotes,
      Map<String, Boolean> sourceReportedByUser,
      Set<PostLabelType> computedLabels) {

    Post post = recommendedPost.getPost();

    String authorId = post.isExternalPost() ? null : post.getUser().getId().toString();
    String username =
        post.isExternalPost()
            ? post.getExternalAuthor().getDisplayName()
            : post.getUser().getUsername();
    String profilePic =
        post.isExternalPost()
            ? post.getExternalAuthor().getAvatarUrl()
            : post.getUser().getProfilePicture();

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
                                    sourceReportedByUser.get(post.getSource().getSourceId()))))
                .toList()
            : List.of();

    List<FeedPostResponse.CommentResponse> comments =
        post.getComments().stream().map(this::toCommentResponse).toList();

    List<String> topicIds =
        post.getPostTopics().stream().map(pt -> pt.getTopic().getName()).toList();

    return new FeedPostResponse(
        post.getPostId(),
        authorId,
        username,
        profilePic,
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
        postReportedByUser != null
            && Boolean.TRUE.equals(postReportedByUser.get(post.getPostId())));
  }

  public FeedPostResponse toFeedPostResponse(
      Post post, Map<String, String> postVotes, Map<String, String> sourceVotes) {
    String authorId = post.isExternalPost() ? null : post.getUser().getId().toString();
    String username =
        post.isExternalPost()
            ? post.getExternalAuthor().getDisplayName()
            : post.getUser().getUsername();
    String profilePic =
        post.isExternalPost()
            ? post.getExternalAuthor().getAvatarUrl()
            : post.getUser().getProfilePicture();

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
                            false))
                .toList()
            : List.of();

    List<FeedPostResponse.CommentResponse> comments =
        post.getComments().stream().map(this::toCommentResponse).toList();

    List<String> topicIds =
        post.getPostTopics().stream().map(pt -> pt.getTopic().getName()).toList();

    return new FeedPostResponse(
        post.getPostId(),
        authorId,
        username,
        profilePic,
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
        false);
  }

  public FeedPostResponse.CommentResponse toCommentResponse(Comment comment) {
    return new FeedPostResponse.CommentResponse(
        comment.getCommentId(),
        comment.getUser().getId().toString(),
        comment.getUser().getUsername(),
        comment.getUser().getProfilePicture(),
        comment.getContent(),
        comment.getCreatedAt(),
        comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null);
  }
}

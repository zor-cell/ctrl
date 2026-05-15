package at.ac.tuwien.inso.ctrl.feed.mapper;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedMapper {

  private final FileStorageService fileStorageService;

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

    List<LabelResponse> labels =
        post.getPostLabels().stream()
            .map(
                pl ->
                    new LabelResponse(
                        pl.getLabel().getLabelId(),
                        pl.getLabel().getName(),
                        pl.getLabel().getEmoji(),
                        pl.getLabel().getExplanation()))
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
                            sourceVotes.get(post.getSource().getSourceId())))
                .toList()
            : List.of();

    List<FeedPostResponse.CommentResponse> comments =
        post.getComments().stream().map(this::toCommentResponse).toList();

    List<String> topicIds =
        post.getPostTopics().stream().map(pt -> pt.getTopic().getName()).toList();

    String mediaRef = post.getMediaRef();

    // Only internal posts receive presigned URLs
    if (!post.isExternalPost() && mediaRef != null) {
      mediaRef = fileStorageService.resolveFileUrl(post.getMediaRef());
    }

    return new FeedPostResponse(
        post.getPostId(),
        authorId,
        username,
        profilePic,
        post.getCaption(),
        mediaRef,
        post.isPublic(),
        post.getUpvotes(),
        post.getDownvotes(),
        postUserVote,
        post.getCreatedAt(),
        topicIds,
        labels,
        sources,
        comments);
  }

  public FeedPostResponse.CommentResponse toCommentResponse(Comment comment) {
    return new FeedPostResponse.CommentResponse(
        comment.getCommentId(),
        comment.getUser().getId().toString(),
        comment.getUser().getUsername(),
        fileStorageService.resolveFileUrl(comment.getUser().getProfilePicture()),
        comment.getContent(),
        comment.getCreatedAt(),
        comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null);
  }
}

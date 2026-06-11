package at.ac.tuwien.inso.ctrl.post.mapper;

import at.ac.tuwien.inso.ctrl.label.entity.PostLabelType;
import at.ac.tuwien.inso.ctrl.post.dto.response.*;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMapper {
  public PostResponse toResponse(Post post) {
    String sourceId = post.getSource() != null ? post.getSource().getSourceId() : null;
    List<LabelResponse> labelIds = List.of();

    List<String> topicIds =
            post.getPostTopics().stream()
                    .map(postTopic -> postTopic.getTopic().getTopicId())
                    .sorted(Comparator.naturalOrder())
                    .toList();
    List<CommentResponse> comments =
            post.getComments().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt))
                    .map(this::toCommentResponse)
                    .toList();

    return new PostResponse(
            post.getPostId(),
            post.getUser().getId(),
            post.getUser().getUsername(),
            sourceId,
            toSourceResponse(post.getSource()),
            post.getCaption(),
            post.getMediaRef(),
            post.isPublic(),
            post.isAiGenerated(),
            labelIds,
            topicIds,
            comments,
            post.getUpvotes(),
            post.getDownvotes(),
            post.getCreatedAt());
  }

  public PostResponse toResponse(Post post, Set<PostLabelType> computedLabels) {
    String sourceId = post.getSource() != null ? post.getSource().getSourceId() : null;

    List<LabelResponse> labels =
            computedLabels.stream()
                    .map(
                            type ->
                                    new LabelResponse(
                                            type.name(), type.getName(), type.getEmoji(), type.getExplanation()))
                    .toList();

    List<String> topicIds =
            post.getPostTopics().stream()
                    .map(postTopic -> postTopic.getTopic().getTopicId())
                    .sorted(Comparator.naturalOrder())
                    .toList();

    List<CommentResponse> comments =
            post.getComments().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt))
                    .map(this::toCommentResponse)
                    .toList();

    return new PostResponse(
            post.getPostId(),
            post.getUser().getId(),
            post.getUser().getUsername(),
            sourceId,
            toSourceResponse(post.getSource()),
            post.getCaption(),
            post.getMediaRef(),
            post.isPublic(),
            post.isAiGenerated(),
            labels,
            topicIds,
            comments,
            post.getUpvotes(),
            post.getDownvotes(),
            post.getCreatedAt());
  }

  public DetailedPostResponse toDetailedPostResponse(
          Post post, PostVote postVote, Set<PostLabelType> computedLabels) {
    String userId =
            post.isExternalPost()
                    ? post.getExternalAuthor().getExternalAuthorId()
                    : post.getUser().getId().toString();
    String username =
            post.isExternalPost()
                    ? post.getExternalAuthor().getDisplayName()
                    : post.getUser().getUsername();
    String avatarUrl =
            post.isExternalPost()
                    ? post.getExternalAuthor().getAvatarUrl()
                    : post.getUser().getProfilePicture();
    String sourceId = post.getSource() != null ? post.getSource().getSourceId() : null;
    List<LabelResponse> labels =
            computedLabels.stream()
                    .map(
                            type ->
                                    new LabelResponse(
                                            type.name(), type.getName(), type.getEmoji(), type.getExplanation()))
                    .toList();
    List<String> topicIds =
            post.getPostTopics().stream()
                    .map(postTopic -> postTopic.getTopic().getTopicId())
                    .sorted(Comparator.naturalOrder())
                    .toList();
    List<CommentResponse> comments =
            post.getComments().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt))
                    .map(this::toCommentResponse)
                    .toList();
    String userVote = postVote == null ? null : postVote.getVoteType();
    String userProfileUrl = post.isExternalPost() ? post.getExternalAuthor().getProfileUrl() : null;

    return new DetailedPostResponse(
            post.getPostId(),
            userId,
            username,
            avatarUrl,
            sourceId,
            toSourceResponse(post.getSource()),
            post.getCaption(),
            post.getMediaRef(),
            post.isPublic(),
            post.isExternalPost(),
            userProfileUrl,
            post.isAiGenerated(),
            labels,
            topicIds,
            comments,
            post.getUpvotes(),
            post.getDownvotes(),
            userVote,
            post.getCreatedAt());
  }

  public PostPreviewResponse toPostPreviewResponse(Post post) {
    String userId =
            post.isExternalPost()
                    ? post.getExternalAuthor().getExternalAuthorId()
                    : post.getUser().getId().toString();
    String username =
            post.isExternalPost()
                    ? post.getExternalAuthor().getDisplayName()
                    : post.getUser().getUsername();
    String avatarUrl =
            post.isExternalPost()
                    ? post.getExternalAuthor().getAvatarUrl()
                    : post.getUser().getProfilePicture();
    String userProfileUrl = post.isExternalPost() ? post.getExternalAuthor().getProfileUrl() : null;
    return new PostPreviewResponse(
            post.getPostId(),
            post.getCaption(),
            post.getMediaRef(),
            userId,
            username,
            avatarUrl,
            post.isPublic(),
            post.isExternalPost(),
            userProfileUrl);
  }

  private SourceResponse toSourceResponse(Source source) {
    if (source == null) {
      return null;
    }

    List<SingleSourceResponse> singleSources =
            source.getSingleSources().stream()
                    .sorted(
                            Comparator.comparing(
                                    SingleSource::getTitle, Comparator.nullsLast(String::compareTo)))
                    .map(this::toSingleSourceResponse)
                    .toList();

    return new SourceResponse(
            source.getSourceId(),
            source.getUpvotes(),
            source.getDownvotes(),
            source.getCreatedAt(),
            singleSources);
  }

  private SingleSourceResponse toSingleSourceResponse(SingleSource source) {
    return new SingleSourceResponse(
            source.getSingleSourceId(),
            source.getJournal(),
            source.getAuthor(),
            source.getTitle(),
            source.getUrl(),
            source.getDate());
  }

  private CommentResponse toCommentResponse(Comment comment) {
    return new CommentResponse(
            comment.getCommentId(),
            comment.getUser().getUsername(),
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getParentComment() == null ? null : comment.getParentComment().getCommentId());
  }
}
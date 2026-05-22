package at.ac.tuwien.inso.ctrl.post.mapper;

import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.SingleSourceResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.SourceResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostMapper {
  private final FileStorageService fileStorageService;

  public PostResponse toResponse(Post post) {
    String sourceId = post.getSource() != null ? post.getSource().getSourceId() : null;
    List<String> labelIds =
        post.getPostLabels().stream()
            .map(postLabel -> postLabel.getLabel().getLabelId())
            .sorted(Comparator.naturalOrder())
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

    String mediaRef = post.getMediaRef();
    String mediaThumbnailRef = fileStorageService.resolveThumbnailRef(mediaRef);

    return new PostResponse(
        post.getPostId(),
        post.getUser().getId(),
        post.getUser().getUsername(),
        sourceId,
        toSourceResponse(post.getSource()),
        post.getCaption(),
        mediaRef,
        fileStorageService.resolveFileUrl(mediaRef),
        mediaThumbnailRef,
        fileStorageService.resolveFileUrl(mediaThumbnailRef),
        post.isPublic(),
        post.isAiGenerated(),
        labelIds,
        topicIds,
        comments,
        post.getUpvotes(),
        post.getDownvotes(),
        post.getCreatedAt());
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

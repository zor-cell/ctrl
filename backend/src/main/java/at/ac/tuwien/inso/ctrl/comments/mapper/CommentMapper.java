package at.ac.tuwien.inso.ctrl.comments.mapper;

import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentMapper {

  public CommentResponse toResponse(
      Comment comment,
      Map<String, List<Comment>> repliesByParent,
      Map<String, Boolean> commentsReportedByUser) {
    List<CommentResponse> nestedReplies =
        repliesByParent.getOrDefault(comment.getCommentId(), List.of()).stream()
            .map(reply -> toResponse(reply, repliesByParent, commentsReportedByUser))
            .toList();

    return new CommentResponse(
        comment.getCommentId(),
        comment.getUser().getId(),
        comment.getUser().getUsername(),
        comment.getUser().getProfilePicture(),
        comment.getCreatedAt().toString(),
        comment.getContent(),
        comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null,
        comment.isEdited(),
        nestedReplies,
        commentsReportedByUser != null
            && Boolean.TRUE.equals(commentsReportedByUser.get(comment.getCommentId())));
  }
}

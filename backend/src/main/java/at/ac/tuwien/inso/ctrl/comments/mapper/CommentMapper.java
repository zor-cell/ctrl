package at.ac.tuwien.inso.ctrl.comments.mapper;

import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    public CommentResponse toResponse(Comment comment, Map<String, List<Comment>> repliesByParent) {
        List<CommentResponse> nestedReplies = repliesByParent
                .getOrDefault(comment.getCommentId(), List.of())
                .stream()
                .map(reply -> toResponse(reply, repliesByParent))
                .toList();

        return new CommentResponse(
                comment.getCommentId(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getUser().getProfilePicture(),  // adjust to your actual User field name
                comment.getCreatedAt().toString(),
                comment.getContent(),
                comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null,
                comment.isEdited(),
                nestedReplies
        );
    }
}

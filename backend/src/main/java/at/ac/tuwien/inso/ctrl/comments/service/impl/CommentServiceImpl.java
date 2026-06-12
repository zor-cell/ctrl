package at.ac.tuwien.inso.ctrl.comments.service.impl;

import at.ac.tuwien.inso.ctrl.comments.dto.request.CreateCommentRequest;
import at.ac.tuwien.inso.ctrl.comments.dto.request.UpdateCommentRequest;
import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.comments.mapper.CommentMapper;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.comments.service.CommentService;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import at.ac.tuwien.inso.ctrl.user.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final CommentMapper commentMapper;
  private final BlockService blockService;

  @Override
  @Transactional(readOnly = true)
  public List<CommentResponse> getCommentsForPost(String postId, User currentUser) {
    if (!postRepository.existsById(postId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
    }

    List<Comment> all =
        commentRepository.findByPost_PostId(postId).stream()
            .filter(c -> c.getUser() != null && !blockService.inBlockedRelationship(currentUser, c.getUser()))
            .filter(c -> c.getParentComment() == null || (
                c.getParentComment().getUser() != null &&
                !blockService.inBlockedRelationship(currentUser, c.getParentComment().getUser())))
            .toList();

    // Group replies by their parent's commentId
    Map<String, List<Comment>> repliesByParent =
        all.stream()
            .filter(c -> c.getParentComment() != null)
            .collect(Collectors.groupingBy(c -> c.getParentComment().getCommentId()));

    // Return only top-level comments; replies are nested inside recursively
    return all.stream()
        .filter(c -> c.getParentComment() == null)
        .map(c -> commentMapper.toResponse(c, repliesByParent))
        .toList();
  }

  @Override
  @Transactional
  public CommentResponse addComment(String postId, CreateCommentRequest request, String username) {
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    Comment parentComment = null;
    if (request.parentCommentId() != null) {
      parentComment =
          commentRepository
              .findById(request.parentCommentId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND, "Parent comment not found"));

      // Ensure the parent comment actually belongs to the same post
      if (!parentComment.getPost().getPostId().equals(postId)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Parent comment does not belong to this post");
      }
    }

    Comment comment =
        Comment.builder()
            .commentId(UUID.randomUUID().toString())
            .post(post)
            .user(user)
            .content(request.content())
            .createdAt(LocalDateTime.now())
            .edited(false)
            .parentComment(parentComment)
            .build();

    commentRepository.save(comment);
    return commentMapper.toResponse(comment, Map.of());
  }

  @Override
  @Transactional
  public CommentResponse updateComment(
      String commentId, UpdateCommentRequest request, String username) {
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

    if (!comment.getUser().getUsername().equals(username)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You are not allowed to edit this comment");
    }

    comment.setContent(request.content());
    comment.setEdited(true);

    commentRepository.save(comment);
    return commentMapper.toResponse(comment, Map.of());
  }

  @Override
  @Transactional
  public void deleteComment(String commentId, String username) {
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

    if (!comment.getUser().getUsername().equals(username)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You are not allowed to delete this comment");
    }

    // CascadeType.ALL + orphanRemoval on the replies relation handles nested deletion automatically
    commentRepository.delete(comment);
  }
}

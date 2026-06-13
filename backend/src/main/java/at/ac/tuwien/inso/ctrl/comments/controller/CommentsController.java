package at.ac.tuwien.inso.ctrl.comments.controller;

import at.ac.tuwien.inso.ctrl.comments.dto.request.CreateCommentRequest;
import at.ac.tuwien.inso.ctrl.comments.dto.request.UpdateCommentRequest;
import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.comments.service.CommentService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommentsController {

  private final CommentService commentService;

  /**
   * Returns all top-level comments for a post, each carrying their nested replies.
   *
   * <p>GET /posts/{postId}/comments
   *
   * @param postId the unique identifier of the post
   * @return a flat list of {@link CommentResponse} objects (replies are nested inside each comment)
   */
  @GetMapping("/posts/{postId}/comments")
  public List<CommentResponse> getComments(
      @PathVariable String postId, @AuthenticationPrincipal User user) {
    return commentService.getCommentsForPost(postId, user);
  }

  /**
   * Creates a new comment (or reply) on a post for the authenticated user. Set {@code
   * parentCommentId} in the request body to post a nested reply.
   *
   * <p>POST /posts/{postId}/comments Body: { "content": "...", "parentCommentId": "<id> | null" }
   *
   * @param postId the unique identifier of the post to comment on
   * @param request the request body containing the comment content and optional parent id
   * @param userDetails the currently authenticated user, injected via Spring Security
   * @return the created comment as {@link CommentResponse} with HTTP 201
   */
  @PostMapping("/posts/{postId}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse addComment(
      @PathVariable String postId,
      @Valid @RequestBody CreateCommentRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    return commentService.addComment(postId, request, userDetails.getUsername());
  }

  /**
   * Updates the content of an existing comment. Only the original author may edit their comment.
   *
   * <p>PUT /comments/{commentId} Body: { "content": "..." }
   *
   * @param commentId the unique identifier of the comment to update
   * @param request the request body containing the new content
   * @param userDetails the currently authenticated user, injected via Spring Security
   * @return the updated comment as {@link CommentResponse}
   */
  @PutMapping("/comments/{commentId}")
  public CommentResponse updateComment(
      @PathVariable String commentId,
      @Valid @RequestBody UpdateCommentRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    return commentService.updateComment(commentId, request, userDetails.getUsername());
  }

  /**
   * Deletes a comment. Only the original author may delete their own comment. Deleting a parent
   * comment also removes all its nested replies.
   *
   * <p>DELETE /comments/{commentId}
   *
   * @param commentId the unique identifier of the comment to delete
   * @param userDetails the currently authenticated user, injected via Spring Security
   */
  @DeleteMapping("/comments/{commentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteComment(
      @PathVariable String commentId, @AuthenticationPrincipal UserDetails userDetails) {
    commentService.deleteComment(commentId, userDetails.getUsername());
  }
}

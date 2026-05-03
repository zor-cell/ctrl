package at.ac.tuwien.inso.ctrl.comments.service;

import at.ac.tuwien.inso.ctrl.comments.dto.request.CreateCommentRequest;
import at.ac.tuwien.inso.ctrl.comments.dto.request.UpdateCommentRequest;
import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import java.util.List;

public interface CommentService {

  /** Returns all comments for the given post, with replies nested inside each top-level comment. */
  List<CommentResponse> getCommentsForPost(String postId);

  /**
   * Creates a new comment (or reply) on the given post authored by {@code username}. Throws if
   * {@code postId} does not exist or if the referenced {@code parentCommentId} does not belong to
   * the same post.
   */
  CommentResponse addComment(String postId, CreateCommentRequest request, String username);

  /**
   * Updates the content of an existing comment. Throws if the comment does not exist or if {@code
   * username} is not the original author.
   */
  CommentResponse updateComment(String commentId, UpdateCommentRequest request, String username);

  /**
   * Deletes a comment and all its nested replies. Throws if the comment does not exist or if {@code
   * username} is not the original author.
   */
  void deleteComment(String commentId, String username);
}

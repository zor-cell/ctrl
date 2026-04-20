package at.ac.tuwien.inso.ctrl.post.service;

import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;

public interface PostService {

  /**
   * Creates a new post for the authenticated user.
   *
   * @param request  the validated post creation request
   * @param username the username of the authenticated user
   * @return the created post as response DTO
   */
  PostResponse createPost(CreatePostRequest request, String username);

  /**
   * Processes an upvote or downvote for a post.
   * Submitting the same vote again toggles (removes) the existing vote.
   *
   * @param postId   the ID of the post to vote on
   * @param request  contains {@code voteType} ({@code "up"} or {@code "down"})
   * @param username the username of the currently authenticated user
   * @return updated vote counts and the user's current vote state
   */
  VoteResponse votePost(String postId, VoteRequest request, String username);
}
package at.ac.tuwien.inso.ctrl.post.service;

import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.DetailedPostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;

public interface PostService {

  /**
   * Creates a new post for the authenticated user.
   *
   * @param request the validated post creation request
   * @param username the username of the authenticated user
   * @return the created post as response DTO
   */
  PostResponse createPost(CreatePostRequest request, String username);

  /**
   * Fetches a post.
   *
   * @param postId the Id of the requested post
   * @param username the username of the authenticated user
   * @return the post as response DTO
   */
  DetailedPostResponse getPostById(String postId, String username);

  /**
   * Processes an upvote or downvote for a post. Submitting the same vote again toggles (removes)
   * the existing vote.
   *
   * @param postId the ID of the post to vote on
   * @param request contains {@code voteType} ({@code "up"} or {@code "down"})
   * @param username the username of the currently authenticated user
   * @return updated vote counts and the user's current vote state
   */
  VoteResponse votePost(String postId, VoteRequest request, String username);

  /**
   * Returns all posts created by the authenticated user.
   *
   * @param username the username of the authenticated user
   * @return posts created by the user ordered from newest to oldest
   */
  List<PostResponse> getPostsForUser(String username);

  /**
   * Returns profile posts for the given user.
   *
   * @param profileUsername the profile owner
   * @param viewer the currently authenticated user
   * @return posts ordered from newest to oldest
   */
  List<PostResponse> getProfilePostsForUser(String profileUsername, User viewer);

  /**
   * Deletes a post created by the authenticated user.
   *
   * @param postId the ID of the post to delete
   * @param username the username of the authenticated user
   */
  void deletePost(String postId, String username);
}

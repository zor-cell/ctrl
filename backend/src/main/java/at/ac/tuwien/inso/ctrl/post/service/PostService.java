package at.ac.tuwien.inso.ctrl.post.service;

import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;

public interface PostService {

  /**
   * Creates a new post for the authenticated user.
   *
   * @param request the validated post creation request
   * @param username the username of the authenticated user
   * @return the created post as response DTO
   */
  PostResponse createPost(CreatePostRequest request, String username);
}

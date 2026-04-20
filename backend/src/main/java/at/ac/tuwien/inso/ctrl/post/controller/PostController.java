package at.ac.tuwien.inso.ctrl.post.controller;

import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;
import at.ac.tuwien.inso.ctrl.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

  private final PostService postService;

  /**
   * Creates a new post for the authenticated user.
   *
   * POST /posts
   *
   * @param request     the request body containing all data for the new post
   * @param userDetails the currently authenticated user, injected via Spring Security
   * @return the created post as {@link PostResponse} with HTTP 201 Created
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PostResponse createPost(
          @Valid @RequestBody CreatePostRequest request,
          @AuthenticationPrincipal UserDetails userDetails) {
    return postService.createPost(request, userDetails.getUsername());
  }

  /**
   * Casts an upvote or downvote on a post for the authenticated user.
   * Sending the same vote type again toggles (removes) the existing vote.
   *
   * PUT /posts/{postId}/vote
   * Body: { "voteType": "up" | "down" }
   *
   * @param postId      the unique identifier of the post to vote on
   * @param request     the request body containing the vote type ('up' or 'down')
   * @param userDetails the currently authenticated user, injected via Spring Security
   * @return the updated vote counts and the user's current vote state as {@link VoteResponse}
   */

  @PutMapping("/{postId}/vote")
  @ResponseStatus(HttpStatus.OK)
  public VoteResponse votePost(
          @PathVariable String postId,
          @Valid @RequestBody VoteRequest request,
          @AuthenticationPrincipal UserDetails userDetails) {
    return postService.votePost(postId, request, userDetails.getUsername());
  }
}
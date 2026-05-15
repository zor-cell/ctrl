package at.ac.tuwien.inso.ctrl.post.controller;

import at.ac.tuwien.inso.ctrl.post.dto.request.PostViewBatchRequest;
import at.ac.tuwien.inso.ctrl.post.service.PostViewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostViewController {

  private final PostViewService postViewService;

  /**
   * Records a batch of post views for the authenticated user. Already-seen posts are silently
   * ignored (idempotent).
   *
   * <p>POST /posts/views Body: { "views": [{ "postId": "...", "durationMs": 3200 }, ...] }
   *
   * @param request the batch of post IDs and view durations sent by the frontend
   * @param userDetails the currently authenticated user, injected via Spring Security
   */
  @PostMapping("/views")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void recordViews(
      @Valid @RequestBody PostViewBatchRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    postViewService.recordViews(request, userDetails.getUsername());
  }
}

package at.ac.tuwien.inso.ctrl.feed.controller;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import java.util.List;

import at.ac.tuwien.inso.ctrl.session.annotations.CheckSessionLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

  private final FeedService feedService;

  /**
   * Returns a paginated list of posts for the feed. This endpoint is publicly accessible –
   * authentication is optional. If a token is provided, user-specific data (e.g. vote state) is
   * included in the response.
   *
   * <p>GET /feed?page={page}&postsPerPage={postsPerPage}
   *
   * @param page zero-based page index (default: 0)
   * @param postsPerPage number of posts per page (default: 20)
   * @param user the currently authenticated user
   * @return a list of {@link FeedPostResponse} objects with HTTP 200 OK
   */
  @GetMapping
  @CheckSessionLimits
  public ResponseEntity<List<FeedPostResponse>> getFeed(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int postsPerPage,
      @RequestParam(defaultValue = "true") boolean publicFeed,
      @AuthenticationPrincipal UserDetails user) {
    return ResponseEntity.ok(
        feedService.getFeed(page, postsPerPage, publicFeed, user.getUsername()));
  }
}

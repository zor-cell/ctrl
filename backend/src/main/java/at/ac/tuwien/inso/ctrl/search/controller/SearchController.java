package at.ac.tuwien.inso.ctrl.search.controller;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.search.dto.PostSearchRequest;
import at.ac.tuwien.inso.ctrl.search.dto.UserSearchRequest;
import at.ac.tuwien.inso.ctrl.search.service.SearchService;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

  private final SearchService searchService;

  /**
   * Searches for posts based on query embedding similarity with optional topic filtering.
   * The search uses a candidate pool approach where the most similar posts to the query
   * embedding are selected, then algorithmic scoring is applied to rank results.
   *
   * Requires either a query (3+ characters) or at least one topic ID.
   *
   * @param request The search request containing query, topics, and pagination
   * @param user The currently authenticated user
   * @return List of feed posts matching the search criteria
   */
  @PostMapping("/posts")
  public ResponseEntity<List<FeedPostResponse>> searchPosts(
      @Valid @RequestBody PostSearchRequest request,
      @AuthenticationPrincipal User user) {

    List<FeedPostResponse> results = searchService.searchPosts(request, user);
    return ResponseEntity.ok(results);
  }

  /**
   * Searches for users based on username matching with optional relationship filters.
   *
   * @param request The search request containing query and filter options
   * @param currentUser The currently authenticated user
   * @return List of users matching the search criteria
   */
  @PostMapping("/users")
  public ResponseEntity<List<UserSearchResponse>> searchUsers(
      @Valid @RequestBody UserSearchRequest request,
      @AuthenticationPrincipal User currentUser) {

    List<UserSearchResponse> results = searchService.searchUsers(request, currentUser);
    return ResponseEntity.ok(results);
  }
}

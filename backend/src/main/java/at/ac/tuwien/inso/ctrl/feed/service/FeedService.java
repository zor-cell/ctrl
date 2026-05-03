package at.ac.tuwien.inso.ctrl.feed.service;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;

import java.util.List;

public interface FeedService {

  /**
   * Returns a paginated list of all posts for the feed, sorted by creation date descending.
   * Includes the userVote field indicating whether the given user has voted on each post.
   *
   * @param page the page number (0-based)
   * @param postsPerPage the number of posts per page
   * @param username the username of the currently authenticated user (for userVote lookup)
   * @return list of feed posts
   */
  List<FeedPostResponse> getFeed(int page, int postsPerPage, String username);
}

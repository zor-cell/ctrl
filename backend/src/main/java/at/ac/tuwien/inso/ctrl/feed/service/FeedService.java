package at.ac.tuwien.inso.ctrl.feed.service;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import java.util.List;

public interface FeedService {

    /**
     * Returns a paginated list of all posts for the feed, sorted by creation date descending.
     *
     * @param page the page number (0-based)
     * @param postsPerPage the number of posts per page
     * @return list of feed posts
     */
    List<FeedPostResponse> getFeed(int page, int postsPerPage);
}
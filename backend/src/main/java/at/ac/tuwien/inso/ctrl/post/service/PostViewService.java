package at.ac.tuwien.inso.ctrl.post.service;

import at.ac.tuwien.inso.ctrl.post.dto.request.PostViewBatchRequest;

public interface PostViewService {

    /**
     * Records a batch of post-views for the authenticated user.
     * Posts already marked as seen are silently ignored (idempotent).
     *
     * @param request the batch of post-IDs and view durations
     * @param username the username of the authenticated user
     */
    void recordViews(PostViewBatchRequest request, String username);
}
package at.ac.tuwien.inso.ctrl.source.service;

import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;
import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import at.ac.tuwien.inso.ctrl.source.entity.Source;

public interface SourceService {

  /**
   * Creates a new source from request data when source details are provided.
   *
   * @param createSourceRequest the optional request used to create a new source
   * @return the newly created source, or {@code null} if no source details are provided
   */
  Source createSource(CreateSourceRequest createSourceRequest);

  /**
   * Processes an upvote or downvote for a source. Submitting the same vote again toggles (removes)
   * the existing vote.
   *
   * @param sourceId the ID of the source to vote on
   * @param request contains {@code voteType} ({@code "up"} or {@code "down"})
   * @param username the username of the currently authenticated user
   * @return updated vote counts and the user's current vote state
   */
  VoteResponse voteSource(String sourceId, VoteRequest request, String username);
}

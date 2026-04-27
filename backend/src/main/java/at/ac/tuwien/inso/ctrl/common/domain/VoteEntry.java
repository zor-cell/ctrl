package at.ac.tuwien.inso.ctrl.common.domain;

/**
 * Abstraction over concrete vote entities (e.g. {@code PostVote}, {@code SourceVote}). Required by
 * {@link at.ac.tuwien.inso.ctrl.common.vote.VoteManager} to read the persisted vote type without
 * depending on a specific entity class.
 */
public interface VoteEntry {

  /** Returns {@code "up"} or {@code "down"}. */
  String getVoteType();
}

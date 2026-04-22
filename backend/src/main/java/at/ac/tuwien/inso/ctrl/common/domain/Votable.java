package at.ac.tuwien.inso.ctrl.common.domain;

/**
 * Marker interface for entities that support up-/downvotes.
 * Both {@code Post} and {@code Source} implement this interface so that
 * {@link at.ac.tuwien.inso.ctrl.common.vote.VoteManager} can manipulate vote
 * counts in a type-safe, entity-agnostic way.
 */
public interface Votable {

    Integer getUpvotes();

    Integer getDownvotes();

    void setUpvotes(Integer upvotes);

    void setDownvotes(Integer downvotes);
}
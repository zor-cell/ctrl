package at.ac.tuwien.inso.ctrl.common.vote;

import at.ac.tuwien.inso.ctrl.common.domain.VoteEntry;
import at.ac.tuwien.inso.ctrl.common.domain.Votable;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Stateless component that contains the canonical vote-toggle logic shared by
 * every votable entity (posts, sources, …).
 *
 * Callers supply three {@link Runnable} callbacks that perform the actual
 * repository operations so that {@code VoteManager} stays decoupled from any
 * specific entity or repository type.
 *
 * Toggle semantics
 * No existing vote → create new vote, increment matching counter.
 * Same vote type as existing → delete (toggle off), decrement counter.
 * Different vote type → update vote, swap counters.
 */
@Component
public class VoteManager {

    /**
     * Processes a vote action and updates the entity's vote counters.
     *
     * @param entity        the {@link Votable} entity whose counters will be mutated
     * @param existingVote  the current {@link VoteEntry} for this (entity, user) pair, if any
     * @param newVoteType   {@code "up"} or {@code "down"}
     * @param createVote    persists a brand-new vote record
     * @param deleteVote    removes the existing vote record
     * @param updateVote    updates the existing vote record to the new type
     * @param saveEntity    persists the mutated {@code entity} back to its repository
     * @return a {@link VoteResponse} with the updated counters and the user's current vote state
     */
    public VoteResponse processVote(
            Votable entity,
            Optional<? extends VoteEntry> existingVote,
            String newVoteType,
            Runnable createVote,
            Runnable deleteVote,
            Runnable updateVote,
            Runnable saveEntity) {

        int upvotes   = entity.getUpvotes()   != null ? entity.getUpvotes()   : 0;
        int downvotes = entity.getDownvotes() != null ? entity.getDownvotes() : 0;
        String userVote;

        if (existingVote.isEmpty()) {
            // No prior vote: create and increment
            createVote.run();
            if ("up".equals(newVoteType)) upvotes++;
            else                          downvotes++;
            userVote = newVoteType;

        } else if (existingVote.get().getVoteType().equals(newVoteType)) {
            // Same vote again: toggle off
            deleteVote.run();
            if ("up".equals(newVoteType)) upvotes   = Math.max(0, upvotes   - 1);
            else                          downvotes = Math.max(0, downvotes - 1);
            userVote = null;

        } else {
            // Opposite vote: switch side
            updateVote.run();
            if ("up".equals(newVoteType)) {
                upvotes++;
                downvotes = Math.max(0, downvotes - 1);
            } else {
                downvotes++;
                upvotes = Math.max(0, upvotes - 1);
            }
            userVote = newVoteType;
        }

        entity.setUpvotes(upvotes);
        entity.setDownvotes(downvotes);
        saveEntity.run();

        return new VoteResponse(upvotes, downvotes, userVote);
    }
}
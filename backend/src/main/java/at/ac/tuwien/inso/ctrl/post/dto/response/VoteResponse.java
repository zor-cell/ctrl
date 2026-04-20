package at.ac.tuwien.inso.ctrl.post.dto.response;

public record VoteResponse(
    int upvotes,
    int downvotes,
    String userVote   // "up", "down" or null (if the vote is withdrawn)
) {}

package at.ac.tuwien.inso.ctrl.post.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VoteRequest(
    @NotNull
    @Pattern(regexp = "up|down", message = "voteType must be 'up' or 'down'")
    String voteType
) {}

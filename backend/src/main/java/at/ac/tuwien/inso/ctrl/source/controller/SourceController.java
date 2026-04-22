package at.ac.tuwien.inso.ctrl.source.controller;

import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sources")
@RequiredArgsConstructor
public class SourceController {

    private final SourceService sourceService;

    /**
     * PUT /sources/{sourceId}/vote
     * Body: { "voteType": "up" | "down" }
     *
     * Returns the current vote counts and the userVote status.
     * The same vote again = toggle (vote is cancelled).
     */
    @PutMapping("/{sourceId}/vote")
    @ResponseStatus(HttpStatus.OK)
    public VoteResponse voteSource(
            @PathVariable String sourceId,
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return sourceService.voteSource(sourceId, request, userDetails.getUsername());
    }
}
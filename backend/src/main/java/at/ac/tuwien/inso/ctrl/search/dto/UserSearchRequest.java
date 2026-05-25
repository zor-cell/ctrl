package at.ac.tuwien.inso.ctrl.search.dto;

import jakarta.validation.constraints.NotBlank;

public record UserSearchRequest(
    @NotBlank String query,
    boolean excludeFriends,
    boolean excludeFollowed,
    boolean verifiedOnly) {}

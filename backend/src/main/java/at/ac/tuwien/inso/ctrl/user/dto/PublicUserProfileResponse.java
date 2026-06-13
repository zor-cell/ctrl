package at.ac.tuwien.inso.ctrl.user.dto;

import java.util.UUID;

public record PublicUserProfileResponse(
    UUID id,
    String username,
    String name,
    String bio,
    String profilePicture,
    boolean isVerified,
    boolean deleted,
    boolean canSeePrivatePosts,
    boolean wasReportedByUser) {}

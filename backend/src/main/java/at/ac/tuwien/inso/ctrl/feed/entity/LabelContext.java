package at.ac.tuwien.inso.ctrl.feed.entity;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record LabelContext(
        Set<UUID> followedIds,       // direct follows by the user
        Set<UUID> secondDegreeIds,   // Extended Network
        int upvoteThreshold,         // 95th percentile across all posts
        LocalDateTime now
) {
}

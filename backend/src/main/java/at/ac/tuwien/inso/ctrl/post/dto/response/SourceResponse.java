package at.ac.tuwien.inso.ctrl.post.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record SourceResponse(
    String sourceId,
    Integer upvotes,
    Integer downvotes,
    LocalDateTime createdAt,
    List<SingleSourceResponse> singleSources) {}

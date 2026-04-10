package at.ac.tuwien.inso.ctrl.source.dto.request;

import jakarta.validation.Valid;
import java.util.List;

public record CreateSourceRequest(
    Integer upvotes, Integer downvotes, @Valid List<CreateSingleSourceRequest> singleSources) {}

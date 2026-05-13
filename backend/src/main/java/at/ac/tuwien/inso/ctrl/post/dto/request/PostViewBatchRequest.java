package at.ac.tuwien.inso.ctrl.post.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PostViewBatchRequest(
        @NotNull @Valid List<PostViewRequest> views) {}
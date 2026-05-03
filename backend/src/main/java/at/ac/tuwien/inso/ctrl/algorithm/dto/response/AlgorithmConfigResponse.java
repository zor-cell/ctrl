package at.ac.tuwien.inso.ctrl.algorithm.dto.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AlgorithmConfigResponse(
        @NotNull @Min(0) @Max(100) Integer weightRecency,
        @NotNull @Min(0) @Max(100) Integer weightExploration,
        @NotNull @Min(0) @Max(100) Integer weightSource,
        @NotNull @Min(0) @Max(100) Integer weightTrending,
        @NotNull @Min(0) @Max(100) Integer weightTopic
) {}

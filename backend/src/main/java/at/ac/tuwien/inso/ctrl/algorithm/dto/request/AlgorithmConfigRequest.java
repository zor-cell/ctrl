package at.ac.tuwien.inso.ctrl.algorithm.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AlgorithmConfigRequest(
    @NotNull @Min(0) @Max(100) Integer weightRecency,
    @NotNull @Min(0) @Max(100) Integer weightExploration,
    @NotNull @Min(0) @Max(100) Integer weightSourceQuality,
    @NotNull @Min(0) @Max(100) Integer weightTrending,
    @NotNull @Min(0) @Max(100) Integer weightTopic,
    @NotNull @Size(max = 10) List<String> topics) {}

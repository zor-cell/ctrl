package at.ac.tuwien.inso.ctrl.algorithm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AlgorithmConfigDto(
    @NotNull @Min(0) @Max(100) Integer weightInteraction,
    @NotNull @Min(0) @Max(100) Integer weightRecency,
    @NotNull @Min(0) @Max(100) Integer weightExploration,
    @NotNull @Min(0) @Max(100) Integer weightSourceQuality,
    @NotNull @Min(0) @Max(100) Integer weightPopularity,
    @NotNull @Min(0) @Max(100) Integer weightTopic,
    @NotNull @Size(max = 5) List<String> topics) {}

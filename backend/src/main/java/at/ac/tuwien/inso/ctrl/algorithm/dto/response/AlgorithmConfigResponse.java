package at.ac.tuwien.inso.ctrl.algorithm.dto.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AlgorithmConfigResponse(
        Integer weightRecency,
        Integer weightExploration,
        Integer weightSourceQuality,
        Integer weightTrending,
        Integer weightTopic,
        List<TopicResponse> topics
) {}

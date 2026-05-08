package at.ac.tuwien.inso.ctrl.post.dto.response;

public record PostExplanation(
    Double scoreInteraction,
    Double scoreTopic,
    Double scoreRecency,
    Double scoreTrending,
    Double scoreSource,
    Double scoreExploration,
    Double totalScore) {}

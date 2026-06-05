package at.ac.tuwien.inso.ctrl.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SessionConfigDto(
    @NotNull @Min(0) Integer timeLimitMinutes,
    @NotNull @Min(0) Integer postLimit,
    @NotNull boolean isForcedCutoff) {}

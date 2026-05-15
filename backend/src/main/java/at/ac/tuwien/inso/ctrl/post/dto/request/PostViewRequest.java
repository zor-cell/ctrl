package at.ac.tuwien.inso.ctrl.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PostViewRequest(@NotBlank String postId, @Positive long durationMs) {}

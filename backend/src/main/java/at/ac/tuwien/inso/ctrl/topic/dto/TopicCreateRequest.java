package at.ac.tuwien.inso.ctrl.topic.dto;

import jakarta.validation.constraints.NotBlank;

public record TopicCreateRequest(@NotBlank String name, String emoji) {}

package at.ac.tuwien.inso.ctrl.topic.dto;

import java.time.LocalDateTime;

public record TopicResponse(String topicId, String name, String emoji, LocalDateTime createdAt) {}

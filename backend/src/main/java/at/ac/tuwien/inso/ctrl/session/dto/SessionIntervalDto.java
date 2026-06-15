package at.ac.tuwien.inso.ctrl.session.dto;

import java.time.LocalDateTime;

public record SessionIntervalDto(LocalDateTime startTime, LocalDateTime endTime) {}

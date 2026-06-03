package at.ac.tuwien.inso.ctrl.session.dto;

import java.time.LocalDate;

public record WeeklySessionDayDto(LocalDate date, long totalMinutes, long sessionCount) {}

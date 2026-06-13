package at.ac.tuwien.inso.ctrl.session.dto;

import java.time.LocalDate;

public record LastWeekDayResponse(LocalDate date, long totalMinutes, long sessionCount) {}

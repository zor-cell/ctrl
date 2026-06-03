package at.ac.tuwien.inso.ctrl.session.dto;

public record DayOfWeekAverageDto(
    int dayOfWeek, double avgTotalMinutes, double avgSessionCount, int daysInPeriod) {}

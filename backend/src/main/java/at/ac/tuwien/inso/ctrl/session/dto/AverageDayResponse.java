package at.ac.tuwien.inso.ctrl.session.dto;

public record AverageDayResponse(
    int dayOfWeek, double avgTotalMinutes, double avgSessionCount, int daysInPeriod) {}

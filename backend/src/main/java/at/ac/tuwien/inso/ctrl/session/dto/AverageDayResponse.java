package at.ac.tuwien.inso.ctrl.session.dto;

public record AverageDayResponse(
    String day, double avgTotalMinutes, double avgSessionCount, int daysInPeriod) {}

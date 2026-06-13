package at.ac.tuwien.inso.ctrl.session.dto;

public record AverageTimeBucketResponse(
    String label, double averageTime, int contributingSessions) {}

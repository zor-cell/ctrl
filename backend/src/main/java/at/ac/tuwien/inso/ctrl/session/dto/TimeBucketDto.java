package at.ac.tuwien.inso.ctrl.session.dto;

public record TimeBucketDto(
    int bucketIndex, String timeLabel, double avgMinutes, long contributingSessions) {}

package at.ac.tuwien.inso.ctrl.session.dto;

import java.util.List;

public record AverageDayDetailResponse(
    String title, int dayOfWeek, List<AverageTimeBucketResponse> buckets, int daysInPeriod) {}

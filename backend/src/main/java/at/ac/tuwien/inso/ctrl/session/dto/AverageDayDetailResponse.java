package at.ac.tuwien.inso.ctrl.session.dto;

import java.util.List;

public record AverageDayDetailResponse(
    String dayOfWeek, List<AverageTimeBucketResponse> timeBuckets, int daysInPeriod) {}

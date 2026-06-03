package at.ac.tuwien.inso.ctrl.session.dto;

import java.util.List;

public record DayOfWeekDetailResponse(int dayOfWeek, List<TimeBucketDto> buckets) {}

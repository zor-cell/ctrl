package at.ac.tuwien.inso.ctrl.session.dto;

import java.util.List;

public record AverageWeekDetailResponse(List<AverageDayDetailResponse> days) {}

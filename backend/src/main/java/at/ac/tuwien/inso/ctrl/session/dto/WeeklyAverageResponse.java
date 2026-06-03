package at.ac.tuwien.inso.ctrl.session.dto;

import java.util.List;

public record WeeklyAverageResponse(List<DayOfWeekAverageDto> days) {}

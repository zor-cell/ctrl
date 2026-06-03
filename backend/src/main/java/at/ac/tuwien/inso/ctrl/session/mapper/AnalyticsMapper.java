package at.ac.tuwien.inso.ctrl.session.mapper;

import at.ac.tuwien.inso.ctrl.session.dto.DayOfWeekAverageDto;
import at.ac.tuwien.inso.ctrl.session.dto.WeeklySessionDayDto;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMapper {

  public WeeklySessionDayDto toWeeklySessionDayDto(Object[] row) {
    LocalDate date = ((Date) row[0]).toLocalDate();
    long totalMinutes = ((Number) row[1]).longValue();
    long sessionCount = ((Number) row[2]).longValue();
    return new WeeklySessionDayDto(date, totalMinutes, sessionCount);
  }

  public List<WeeklySessionDayDto> toWeeklySessionDayDtos(List<Object[]> rows) {
    return rows.stream().map(this::toWeeklySessionDayDto).toList();
  }

  public DayOfWeekAverageDto toDayOfWeekAverageDto(Object[] row) {
    int dayOfWeek = ((Number) row[0]).intValue();
    double avgTotalMinutes = ((Number) row[1]).doubleValue();
    double avgSessionCount = ((Number) row[2]).doubleValue();
    int daysInPeriod = ((Number) row[3]).intValue();
    return new DayOfWeekAverageDto(dayOfWeek, avgTotalMinutes, avgSessionCount, daysInPeriod);
  }

  public List<DayOfWeekAverageDto> toDayOfWeekAverageDtos(List<Object[]> rows) {
    return rows.stream().map(this::toDayOfWeekAverageDto).toList();
  }
}

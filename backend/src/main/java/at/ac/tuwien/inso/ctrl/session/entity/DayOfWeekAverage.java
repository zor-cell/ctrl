package at.ac.tuwien.inso.ctrl.session.entity;

public interface DayOfWeekAverage {
  Integer getDayOfWeek();

  Double getAvgMinutes();

  Double getAvgSessionCount();

  Integer getDaysInPeriod();
}

package at.ac.tuwien.inso.ctrl.session.entity;

public interface DayOfWeekAverage {
  Integer getDayOfWeek();

  Double getAvgSeconds();

  Double getAvgSessionCount();

  Integer getDaysInPeriod();
}

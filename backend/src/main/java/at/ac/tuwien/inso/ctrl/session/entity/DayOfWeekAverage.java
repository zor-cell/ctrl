package at.ac.tuwien.inso.ctrl.session.entity;

public interface DayOfWeekAverage {
  Integer getDayOfWeek();

  Long getAvgTotalMinutes();

  Long getAvgSessionCount();

  Integer getDaysInPeriod();
}

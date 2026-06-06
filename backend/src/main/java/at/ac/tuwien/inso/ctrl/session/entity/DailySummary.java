package at.ac.tuwien.inso.ctrl.session.entity;

import java.time.LocalDate;

public interface DailySummary {
  LocalDate getDay();

  Long getTotalMinutes();

  Long getSessionCount();
}

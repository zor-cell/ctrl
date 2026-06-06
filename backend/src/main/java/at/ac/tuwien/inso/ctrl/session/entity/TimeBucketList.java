package at.ac.tuwien.inso.ctrl.session.entity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.List;

public class TimeBucketList {
  private TimeBucket head;

  public TimeBucketList(List<SessionHistory> sessions) {
    this.head = new TimeBucket(LocalTime.of(0, 0), DayOfWeek.MONDAY);

    this.head.distributeSessions(sessions.stream().sorted().toList());
  }

  private class TimeBucket {
    private TimeBucket next;
    private DayOfWeek weekday;
    private LocalTime startTime;
    private LocalTime endTime;
    private int totalMinutes;
    private int contributingSessions;

    private TimeBucket(LocalTime startTime, DayOfWeek weekday) {
      this.weekday = weekday;
      this.startTime = startTime;
      this.endTime = startTime.plusMinutes(15);
      this.totalMinutes = 0;
      this.contributingSessions = 0;

      this.next = new TimeBucket(this.endTime, this.getNextWeekday(endTime, weekday), this);
    }

    private TimeBucket(LocalTime startTime, DayOfWeek weekday, TimeBucket head) {
      if (weekday.equals(DayOfWeek.SUNDAY) && startTime.isAfter(LocalTime.of(23, 44))) {
        this.next = head;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = LocalTime.of(0, 0);
      }
    }

    private DayOfWeek getNextWeekday(LocalTime endTime, DayOfWeek weekDay) {
      return endTime.equals(LocalTime.MIDNIGHT) ? weekDay.plus(1) : weekDay;
    }

    public void distributeSessions(List<SessionHistory> sessions) {
      if (sessions.isEmpty()) {
        return;
      }

      for (SessionHistory s : sessions) {
        if (s.getStart().toLocalTime().isAfter(this.endTime)) {
          // no more sessions for this bucket, skip to the next bucket
          break;
        }
        int overlap =
            Math.min(
                    s.getStart().plusMinutes(s.getMinutes()).get(ChronoField.MINUTE_OF_DAY),
                    this.endTime.get(ChronoField.MINUTE_OF_DAY))
                - Math.max(
                    s.getStart().get(ChronoField.MINUTE_OF_DAY),
                    this.startTime.get(ChronoField.MINUTE_OF_DAY));

        if (overlap > 0) {
          this.addMinutes(overlap);
          this.incrementContributingSessions();
        }

        if (s.getStart().toLocalTime().plusMinutes(s.getMinutes()).isBefore(this.endTime)) {
          // session is fully accounted for, remove for efficiency and correctness
          sessions.remove(s);
        }
      }

      this.next.distributeSessions(sessions);
    }

    public void addMinutes(int min) {
      this.totalMinutes += min;
    }

    public void incrementContributingSessions() {
      this.contributingSessions++;
    }
  }
}

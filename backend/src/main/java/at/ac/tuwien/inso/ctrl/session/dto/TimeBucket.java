package at.ac.tuwien.inso.ctrl.session.dto;

import java.time.LocalTime;
import lombok.Getter;

@Getter
public class TimeBucket {
  private int bucketIndex;
  private LocalTime startTime;
  private int totalMinutes;
  private int contributingSessions;

  public TimeBucket(int bucketIndex, LocalTime startTime) {
    this.bucketIndex = bucketIndex;
    this.startTime = startTime;
    this.totalMinutes = 0;
    this.contributingSessions = 0;
  }

  public void addMinutes(int min) {
    this.totalMinutes += min;
  }

  public void incrementContributingSessions() {
    this.contributingSessions++;
  }
}

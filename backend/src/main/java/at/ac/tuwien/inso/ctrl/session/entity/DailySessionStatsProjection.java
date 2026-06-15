package at.ac.tuwien.inso.ctrl.session.entity;

public interface DailySessionStatsProjection {
  long getTotalSeconds();

  long getTotalPostsViewed();
}

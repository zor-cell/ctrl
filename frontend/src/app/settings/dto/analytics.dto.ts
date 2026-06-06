export interface LastWeekDayResponse {
  date: string;
  totalMinutes: number;
  sessionCount: number;
}

export interface LastWeekResponse {
  days: LastWeekDayResponse[];
}

export interface SessionIntervalDto {
  startTime: string;
  endTime: string;
}

export interface LastWeekDayDetailResponse {
  date: string;
  sessions: SessionIntervalDto[];
}

export interface LastWeekDetailResponse {
  days: LastWeekDayDetailResponse[];
}

export interface AverageDayResponse {
  dayOfWeek: string;
  avgTotalMinutes: number;
  avgSessionCount: number;
  daysInPeriod: number;
}

export interface AverageWeekResponse {
  days: AverageDayResponse[];
}

export interface TimeBucketDto {
  bucketIndex: number;
  timeLabel: string;
  avgMinutes: number;
  contributingSessions: number;
}

export interface AverageDayDetailResponse {
  dayOfWeek: number;
  buckets: TimeBucketDto[];
  daysInPeriod: number;
}

export interface AverageWeekDetailResponse {
  days: AverageDayDetailResponse[];
}

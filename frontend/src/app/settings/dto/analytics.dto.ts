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

export interface LastWeekDayDetailResponse extends DetailedDay {
  date: string;
  sessions: SessionIntervalDto[];
}

export interface AverageWeekDayResponse {
  avgTotalMinutes: number;
  avgSessionCount: number;
  dayOfWeek: number;
  daysInPeriod: number;
}


export interface AverageWeekResponse {
  days: AverageWeekDayResponse[];
}

export interface TimeBucketDto {
  label: string;
  averageTime: number;
  contributingSessions: number;
}

export interface AverageDayDetailResponse extends DetailedDay {
  buckets: TimeBucketDto[];
  dayOfWeek: number;
  daysInPeriod: number;
}


export interface DetailedDay {
  title: string
}

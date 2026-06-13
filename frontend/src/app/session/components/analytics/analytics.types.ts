import {
  WeekDayDetailResponse,
  AverageDayDetailResponse,
  WeekDayResponse,
  AverageWeekDayResponse,
} from '../../dto/analytics.dto';

export type DatasetKey =
  | 'overview-days'
  | 'overview-average'
  | 'detailed-days'
  | 'detailed-average';

export interface DatasetMap {
  'overview-days': WeekDayResponse[];
  'overview-average': AverageWeekDayResponse[];
  'detailed-days': WeekDayDetailResponse[];
  'detailed-average': AverageDayDetailResponse[];
}

export type DatasetResponse = DatasetMap[DatasetKey];

import {
  LastWeekDayDetailResponse,
  AverageDayDetailResponse,
  LastWeekDayResponse,
  AverageWeekDayResponse,
} from '../../dto/analytics.dto';

export type DatasetKey =
  | 'overview-days'
  | 'overview-average'
  | 'detailed-days'
  | 'detailed-average';

export interface DatasetMap {
  'overview-days': LastWeekDayResponse[];
  'overview-average': AverageWeekDayResponse[];
  'detailed-days': LastWeekDayDetailResponse[];
  'detailed-average': AverageDayDetailResponse[];
}

export type DatasetResponse = DatasetMap[DatasetKey];

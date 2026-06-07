import {
  LastWeekResponse,
  AverageWeekResponse,
  LastWeekDayDetailResponse,
  AverageDayDetailResponse,
} from '../../dto/analytics.dto';

export type DatasetKey =
  | 'overview-lastweek'
  | 'overview-average'
  | 'detailed-lastweek'
  | 'detailed-average';

export interface DatasetMap {
  'overview-lastweek': LastWeekResponse;
  'overview-average': AverageWeekResponse;
  'detailed-lastweek': LastWeekDayDetailResponse[];
  'detailed-average': AverageDayDetailResponse[];
}

export type DatasetResponse = DatasetMap[DatasetKey];

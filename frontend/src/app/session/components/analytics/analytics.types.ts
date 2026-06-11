import {
  LastWeekDayDetailResponse,
  AverageDayDetailResponse,
  LastWeekDayResponse,
  AverageWeekDayResponse,
} from '../../dto/analytics.dto';

export type DatasetKey =
  | 'overview-lastweek'
  | 'overview-average'
  | 'detailed-lastweek'
  | 'detailed-average';

export interface DatasetMap {
  'overview-lastweek': LastWeekDayResponse[];
  'overview-average': AverageWeekDayResponse[];
  'detailed-lastweek': LastWeekDayDetailResponse[];
  'detailed-average': AverageDayDetailResponse[];
}

export type DatasetResponse = DatasetMap[DatasetKey];

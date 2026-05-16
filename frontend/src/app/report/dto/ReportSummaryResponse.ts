import {ContentTypeEnum} from './report';

export interface ReportSummaryResponse {
  contentType: ContentTypeEnum;
  reason: string;
  description: string;
}

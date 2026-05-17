import {ContentTypeEnum, ReportStatus} from './report';
import {SourceResponse} from '../../post/dto/PostResponse';

export interface ReportedSourceResponse {
  reportId: string;
  reportReason: string;
  reportDescription: string;
  reportedByUsername: string;
  reportedUsername: string;
  contentType: ContentTypeEnum;
  status: ReportStatus;
  reportedAt: Date;
  sourceResponse: SourceResponse;
}

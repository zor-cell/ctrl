
import {ContentTypeEnum, ReportStatus} from './report';
import {PublicUserProfile} from '../../user/dto/PublicUserProfile';

export interface ReportedAccountResponse {
  profileResponse: PublicUserProfile;
  reportId: string;
  reportReason: string;
  reportDescription: string;
  reportedByUsername: string;
  reportedUsername: string;
  contentType: ContentTypeEnum;
  status: ReportStatus;
  reportedAt: string;
}

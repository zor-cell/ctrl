import { ContentTypeEnum, ReportStatus } from './report';
import { CommentResponse } from '../../post/dto/PostResponse';

export interface ReportedCommentResponse {
  comment: CommentResponse;
  reportId: string;
  reportReason: string;
  reportDescription: string;
  reportedByUsername: string;
  reportedUsername: string;
  contentType: ContentTypeEnum;
  status: ReportStatus;
  reportedAt: string;
}


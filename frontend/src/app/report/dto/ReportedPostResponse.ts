import {ContentTypeEnum, ReportStatus} from './report';
import {LabelResponse} from '../../post/dto/LabelResponse';
import {CommentResponse, SourceResponse} from '../../post/dto/PostResponse';

export interface ReportedPostResponse {
  reportId: string;
  reportReason: string;
  reportDescription: string;
  reportedByUsername: string;
  reportedUsername: string;
  contentType: ContentTypeEnum;
  status: ReportStatus;
  reportedAt: Date;
  postId: string;
  userId: string | null;
  username: string;
  userProfilePic: string;
  caption: string;
  mediaRef: string | null;
  mediaUrl: string | null;
  isPublic: boolean;
  upvotes: number;
  downvotes: number;
  userVote: string | null;
  createdAt: Date;
  topicIds: string[];
  labels: LabelResponse[];
  sources: SourceResponse[];
  comments: CommentResponse[];
}

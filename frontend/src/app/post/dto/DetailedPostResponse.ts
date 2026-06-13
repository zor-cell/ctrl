import { VoteType } from "../post.service";
import { CommentResponse, SourceResponse } from "./PostResponse";

export interface DetailedPostResponse {
  postId: string;
  userId: string;
  username: string;
  userProfilePicture: string;
  userDeleted: boolean;
  sourceId: string | null;
  source: SourceResponse | null;
  caption: string;
  mediaUrl: string | null;
  isPublic: boolean;
  isExternal: boolean;
  userProfileUrl: string | null;
  wasReportedByUser: boolean;
  isAiGenerated: boolean;
  labelIds: string[];
  topicIds: string[];
  comments: CommentResponse[];
  upvotes: number;
  downvotes: number;
  userVote: VoteType;
  createdAt: string;
}

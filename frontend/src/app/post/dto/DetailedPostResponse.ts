import { VoteType } from "../post.service";
import { CommentResponse, SourceResponse } from "./PostResponse";

export interface LabelResponse {
  labelId: string;
  name: string;
  emoji: string;
  explanation: string;
}

export interface DetailedPostResponse {
  postId: string;
  userId: string;
  username: string;
  userProfilePicture: string;
  sourceId: string | null;
  source: SourceResponse | null;
  caption: string;
  mediaUrl: string | null;
  isPublic: boolean;
  isExternal: boolean;
  userProfileUrl: string | null;
  isAiGenerated: boolean;
  labelIds: LabelResponse[];
  topicIds: string[];
  comments: CommentResponse[];
  upvotes: number;
  downvotes: number;
  userVote: VoteType;
  createdAt: string;
}

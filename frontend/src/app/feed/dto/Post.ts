import { PostComment } from './Comment';
import { PostSource } from './PostSource';
import {PostExplanation} from "./PostExplanation";

export interface Post {
  postId: string;
  userId: string;
  username: string;
  userProfilePic: string;
  userDeleted?: boolean;
  createdAt: string;
  caption: string;
  isPublic: boolean;
  likes: number;
  upvotes: number;
  downvotes: number;
  userVote: 'up' | 'down' | null;
  labelIds: [string, string][];
  topicIds: string[];
  videoImageRef: string[];
  isExternal: boolean;
  sourceIds: PostSource[];
  comments: PostComment[];
  explanation: PostExplanation;
  wasReportedByUser: boolean;
}

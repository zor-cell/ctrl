import {PostComment} from './Comment';
import { PostSource } from './PostSource';

export interface Post {
  postId: string;
  userId: string;
  username: string;
  userProfilePic: string;
  createdAt: string;
  caption: string;
  isPublic: boolean;
  whyExplanation: string; // will be changed in the future
  upvotes: number;
  downvotes: number;
  likes: number;
  labelIds: [string, string][];
  topicIds: string[];
  videoImageRef: string[];
  sourceIds: PostSource[];
  comments: PostComment[];
}

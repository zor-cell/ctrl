import { PostComment } from './Comment';
import { PostSource } from './PostSource';

export interface Post {
  postId: string;
  userId: string;
  username: string;
  userProfilePic: string;
  createdAt: string;
  caption: string;
  isPublic: boolean;
  whyExplanation: string;
  likes: number;
  upvotes: number;
  downvotes: number;
  userVote: 'up' | 'down' | null;
  labelIds: [string, string][];
  topicIds: string[];
  videoImageRef: string[];
  sourceIds: PostSource[];
  comments: PostComment[];
}

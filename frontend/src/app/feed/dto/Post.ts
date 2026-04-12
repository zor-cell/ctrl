import {PostComment} from './Comment';

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
  labelIds: string[];
  topicIds: string[];
  videoImageRef: string;
  // sourceIds: string[] | null;
  comments: PostComment[];
}


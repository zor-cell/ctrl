export interface CommentResponse {
  commentId: string;
  userId?: string;
  username: string;
  userProfilePic?: string;
  content: string;
  createdAt: string;
  parentCommentId: string | null;
  edited?: boolean;
  wasReportedByUser: boolean;
  replies?: CommentResponse[];
}

export interface PostResponse {
  postId: string;
  userId: string;
  username: string;
  sourceId: string | null;
  source: SourceResponse | null;
  caption: string;
  mediaRef: string | null;
  isPublic: boolean;
  isAiGenerated: boolean;
  labelIds: string[];
  topicIds: string[];
  comments: CommentResponse[];
  upvotes: number;
  downvotes: number;
  createdAt: string;
}

export interface SingleSourceResponse {
  singleSourceId: string;
  journal: string | null;
  author: string | null;
  title: string | null;
  url: string | null;
  date: string | null;
}

export interface SourceResponse {
  sourceId: string;
  upvotes: number;
  downvotes: number;
  createdAt: string;
  wasReportedByUser: boolean;
  singleSources: SingleSourceResponse[];
}

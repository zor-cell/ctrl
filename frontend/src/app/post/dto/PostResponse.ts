export interface PostResponse {
  postId: string;
  userId: string;
  username: string;
  sourceId: string | null;
  caption: string;
  mediaRef: string | null;
  isPublic: boolean;
  isAiGenerated: boolean;
  labelIds: string[];
  topicIds: string[];
  upvotes: number;
  downvotes: number;
  createdAt: string;
}

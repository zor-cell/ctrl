export interface PostResponse {
  postId: string;
  userId: string;
  username: string;
  sourceId: string | null;
  caption: string;
  mediaRef: string | null;
  isPublic: boolean;
  labelIds: string[];
  upvotes: number;
  downvotes: number;
  createdAt: string;
}

export interface PostComment {
  commentId: string;
  userId: string;
  username: string;
  userProfilePic: string;
  createdAt: string;
  content: string;
  parentCommentId: string | null;
  edited: boolean;
  replies?: PostComment[];
  wasReportedByUser: boolean;
}

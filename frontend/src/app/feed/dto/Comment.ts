export interface PostComment {
  commentId: string;
  userId: string;
  username: string;
  userProfilePic: string;
  userDeleted?: boolean;
  createdAt: string;
  content: string;
  parentCommentId: string | null;
  edited: boolean;
  replies?: PostComment[];
  wasReportedByUser: boolean;
}

export interface PostPreview {
  postId: string;
  caption: string;
  mediaUrl: string | null;
  userId: string;
  username: string;
  userProfilePic: string | null;
  isPublic: boolean;
}

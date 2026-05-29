export interface PostPreview {
  postId: string;
  caption: string;
  mediaUrl: string | null;
  userId: string;
  username: string;
  userProfilePicture: string | null;
  isPublic: boolean;
}

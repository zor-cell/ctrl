export interface SharedPostPayload {
  postId: string;
  username: string;
  caption: string;
  url: string;
  mediaUrl: string | null;
  mediaKind: 'image' | 'video' | null;
}

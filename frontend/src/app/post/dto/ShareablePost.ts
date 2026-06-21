import { Post} from '../../feed/dto/Post';
import { DetailedPostResponse} from './DetailedPostResponse';

export interface MinimalShareablePost {
  postId: string;
  username: string;
  caption: string;
  mediaUrl: string | null;
}

export type ShareablePost = Post | DetailedPostResponse | MinimalShareablePost;

/** Extracts the media URL regardless of which post shape is passed in. */
export function resolveShareMediaUrl(post: ShareablePost): string | null {
  if ('mediaUrl' in post) return post.mediaUrl ?? null;          // DetailedPostResponse
  return (post.videoImageRef?.[0]) ?? null;                      // Post
}

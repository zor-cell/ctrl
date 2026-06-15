import { Post} from '../../feed/dto/Post';
import { DetailedPostResponse} from './DetailedPostResponse';

export type ShareablePost = Post | DetailedPostResponse;

/** Extracts the media URL regardless of which post shape is passed in. */
export function resolveShareMediaUrl(post: ShareablePost): string | null {
  if ('mediaUrl' in post) return post.mediaUrl ?? null;          // DetailedPostResponse
  return (post.videoImageRef?.[0]) ?? null;                      // Post
}

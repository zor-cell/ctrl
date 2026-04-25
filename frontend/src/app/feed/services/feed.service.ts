import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Post } from '../dto/Post';
import { environment } from '../../../environments/environment';
import { PostComment } from '../dto/Comment';
import { PostSource } from '../dto/PostSource';

interface FeedLabelResponse {
  name: string;
  description: string;
}

interface FeedCommentResponse {
  commentId: string;
  userId: string;
  username: string;
  userProfilePic?: string;
  createdAt: string;
  content: string;
  parentCommentId?: string | null;
}

interface FeedPostResponse {
  postId: string;
  userId?: string;
  username: string;
  userProfilePic?: string;
  createdAt: string;
  caption?: string;
  isPublic: boolean;
  upvotes?: number;
  downvotes?: number;
  userVote?: 'up' | 'down' | null;
  labelIds?: FeedLabelResponse[];
  topicIds?: string[];
  mediaRef?: string;
  sourceIds?: PostSource[];
  comments?: FeedCommentResponse[];
}

@Injectable({ providedIn: 'root' })
export class FeedService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/feed`;

  /**
   * Fetches a paginated list of posts for the current user's feed.
   *
   * GET /feed?page={page}&postsPerPage={postsPerPage}
   *
   * @param page - Zero-based page index (default: 0)
   * @param postsPerPage - Number of posts per page (default: 20)
   * @returns A promise resolving to an array of mapped {@link Post} objects
   */

  async getFeed(page = 0, postsPerPage = 20): Promise<Post[]> {
    const response = await firstValueFrom(
      this.httpClient.get<FeedPostResponse[]>(`${this.baseUri}?page=${page}&postsPerPage=${postsPerPage}`)
    );
    return response.map(this.mapToPost);
  }

  /**
   * Maps a raw API response object to a typed {@link Post} model.
   * Applies fallback values for optional fields that may be absent in the response.
   *
   * @param item - Raw response object from the feed API
   * @returns A fully structured {@link Post} object
   */
  private mapToPost(item: FeedPostResponse): Post {
    return {
      postId: item.postId,
      userId: item.userId ?? item.username,
      username: item.username,
      userProfilePic: item.userProfilePic ?? '',
      createdAt: item.createdAt,
      caption: item.caption ?? '',
      isPublic: item.isPublic,
      whyExplanation: '',
      likes: 0,
      upvotes: item.upvotes ?? 0,
      downvotes: item.downvotes ?? 0,
      userVote: item.userVote ?? null,
      labelIds: item.labelIds?.map(label => [label.name, label.description]) ?? [],
      topicIds: item.topicIds ?? [],
      videoImageRef: item.mediaRef ? [item.mediaRef] : [],
      sourceIds: item.sourceIds ?? [],
      comments:
        item.comments?.map((comment): PostComment => ({
          commentId: comment.commentId,
          userId: comment.userId,
          username: comment.username,
          userProfilePic: comment.userProfilePic ?? '',
          createdAt: comment.createdAt,
          content: comment.content,
          parentCommentId: comment.parentCommentId ?? null,
        })) ?? [],
    };
  }
}

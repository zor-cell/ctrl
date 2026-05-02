//TODO: warning should be fixed!
/* eslint-disable @typescript-eslint/no-explicit-any */

import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Post } from '../dto/Post';
import { environment } from '../../../environments/environment';
import { MediaCacheService } from '../../core/services/media-cache.service';

@Injectable({ providedIn: 'root' })
export class FeedService {
  private readonly httpClient = inject(HttpClient);
  private readonly mediaCacheService = inject(MediaCacheService);
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
      this.httpClient.get<any[]>(`${this.baseUri}?page=${page}&postsPerPage=${postsPerPage}`)
    );
    const posts = response.map(item => this.mapToPost(item));
    this.cacheFeedMedia(posts);
    return posts;
  }

  /**
   *
   * Maps a raw API response object to a typed {@link Post} model.
   * Applies fallback values for optional fields that may be absent in the response.
   *
   * @param item - Raw response object from the feed API
   * @returns A fully structured {@link Post} object
   */
  private mapToPost(item: any): Post {
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
      labelIds: item.labelIds?.map((l: any) => [l.name, l.description]) ?? [],
      topicIds: item.topicIds ?? [],
      videoImageRef: item.mediaRef ? [item.mediaRef] : [],
      sourceIds: item.sourceIds ?? [],
      comments:
        item.comments?.map((c: any) => ({
          commentId: c.commentId,
          userId: c.userId,
          username: c.username,
          userProfilePic: c.userProfilePic ?? '',
          createdAt: c.createdAt,
          content: c.content,
          parentCommentId: c.parentCommentId ?? null,
        })) ?? [],
    };
  }

  private cacheFeedMedia(posts: Post[]): void {
    this.mediaCacheService.cacheUrls([
      ...posts.map(post => post.userProfilePic),
      ...posts.flatMap(post => post.videoImageRef),
    ]);
  }
}

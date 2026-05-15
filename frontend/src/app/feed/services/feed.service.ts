//TODO: warning should be fixed!
/* eslint-disable @typescript-eslint/no-explicit-any */

import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {firstValueFrom, map} from 'rxjs';
import { Post } from '../dto/Post';
import { environment } from '../../../environments/environment';
import { MediaCacheKind, MediaCacheService } from '../../core/services/media-cache.service';

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
   * @param isPublic - Fetches the public feed if true, otherwise the private feed
   * @param page - Zero-based page index (default: 0)
   * @param postsPerPage - Number of posts per page (default: 20)
   * @returns A promise resolving to an array of mapped {@link Post} objects
   */

  getFeed(isPublic: boolean, page = 0, postsPerPage = 20) {
    return this.httpClient
      .get<any[]>(`${this.baseUri}?publicFeed=${isPublic}&page=${page}&postsPerPage=${postsPerPage}`)
      .pipe(
        map((response: any[]) => response.map(item => this.mapToPost(item)))
      );
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
      likes: 0,
      upvotes: item.upvotes ?? 0,
      downvotes: item.downvotes ?? 0,
      userVote: item.userVote ?? null,
      labelIds: item.labelIds?.map((l: any) => [l.name, l.explanation]) ?? [],
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
          edited: c.edited ?? false,
          replies: c.replies ?? [],
        })) ?? [],
      explanation: item.explanation
    };
  }

  private async resolveFeedMedia(posts: Post[]): Promise<void> {
    await Promise.all(
      posts.map(async post => {
        post.userProfilePic = await this.resolveMedia(post.userProfilePic, 'image', 'high') ?? '';
        post.videoImageRef = (
          await Promise.all(
            post.videoImageRef.map(mediaUrl =>
              this.resolveMedia(mediaUrl, this.mediaKind(mediaUrl), 'normal')
            )
          )
        ).filter((mediaUrl): mediaUrl is string => mediaUrl !== null);
      })
    );
  }

  private resolveMedia(
    mediaUrl: string | null | undefined,
    kind: MediaCacheKind,
    priority: 'normal' | 'high'
  ): Promise<string | null> {
    return firstValueFrom(this.mediaCacheService.resolveMediaUrl(mediaUrl, kind, priority))
      .catch(() => null);
  }

  private mediaKind(mediaUrl: string): MediaCacheKind {
    return /\.(mp4|webm|mov)(\?|#|$)/i.test(mediaUrl) ? 'video' : 'image';
  }
}

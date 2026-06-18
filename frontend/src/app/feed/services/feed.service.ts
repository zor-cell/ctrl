/* eslint-disable @typescript-eslint/no-explicit-any */

import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, map, switchMap } from 'rxjs';
import { Post } from '../dto/Post';
import { environment } from '../../../environments/environment';
import { MediaCacheKind, MediaCacheService } from '../../core/services/media-cache.service';
import { previewMediaUrl } from '../../core/utils/media-preview-url.utils';

@Injectable({ providedIn: 'root' })
export class FeedService {
  private readonly httpClient = inject(HttpClient);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly baseUri = `${environment.apiUrl}/feed`;

  getFeed(isPublic: boolean, page = 0, postsPerPage = 20) {
    return this.httpClient
      .get<any[]>(`${this.baseUri}?publicFeed=${isPublic}&page=${page}&postsPerPage=${postsPerPage}`)
      .pipe(
        map((response: any[]) => response.map(item => this.mapToPost(item))),
        switchMap(async posts => {
          await this.resolveFeedMedia(posts);
          return posts;
        })
      );
  }

  private mapToPost(item: any): Post {
    return {
      postId:           item.postId,
      userId:           item.userId ?? item.username,
      isExternal:       !item.userId,
      username:         item.username,
      userProfilePic:   item.userProfilePic ?? '',
      userDeleted:      item.userDeleted ?? false,
      createdAt:        item.createdAt,
      caption:          item.caption ?? '',
      isPublic:         item.isPublic,
      likes:            0,
      upvotes:          item.upvotes ?? 0,
      downvotes:        item.downvotes ?? 0,
      userVote:         item.userVote ?? null,
      labelIds:         item.labelIds ?? [],
      topicIds:         item.topicIds ?? [],
      videoImageRef:    this.resolveVideoImageRef(item),
      sourceIds: item.sourceIds?.map((s: any) => ({
        sourceId:          s.sourceId,
        url:               s.url               ?? null,
        upvotes:           s.upvotes           ?? 0,
        downvotes:         s.downvotes         ?? 0,
        userVote:          s.userVote          ?? null,
        wasReportedByUser: s.wasReportedByUser ?? false,
        createdAt:         s.createdAt         ?? null,
        journal:           s.journal           ?? null,
        author:            s.author            ?? null,
        title:             s.title             ?? null,
        date:              s.date              ?? null,
      })) ?? [],
      comments: item.comments?.map((c: any) => ({
        commentId:       c.commentId,
        userId:          c.userId,
        username:        c.username,
        userProfilePic:  c.userProfilePic ?? '',
        userDeleted:     c.userDeleted    ?? false,
        createdAt:       c.createdAt,
        content:         c.content,
        parentCommentId: c.parentCommentId ?? null,
        edited:          c.edited          ?? false,
        replies:         c.replies         ?? [],
      })) ?? [],
      explanation:       item.explanation,
      wasReportedByUser: item.wasReportedByUser,
      userProfileUrl: item.userProfileUrl,
    };
  }

  private async resolveFeedMedia(posts: Post[]): Promise<void> {
    await Promise.all(
      posts.map(async post => {
        post.userProfilePic = await this.resolveMedia(post.userProfilePic, 'image', 'high') ?? '';
        post.videoImageRef = (
          await Promise.all(
            post.videoImageRef.map(async mediaUrl => {
              const resolvedMediaUrl = await this.resolveMedia(mediaUrl, this.mediaKind(mediaUrl), 'normal');
              return previewMediaUrl(resolvedMediaUrl);
            })
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

  private resolveVideoImageRef(item: any): string[] {
    if (item.videoImageRef?.length > 0) return item.videoImageRef;
    if (item.mediaRef) return [item.mediaRef];
    return [];
  }
}

import {inject, Injectable} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Post } from '../dto/Post';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FeedService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/feed`;

  async getFeed(page: number = 0, postsPerPage: number = 20): Promise<Post[]> {
    const response = await firstValueFrom(
      this.httpClient.get<any[]>(`${this.baseUri}?page=${page}&postsPerPage=${postsPerPage}`)
    );
    return response.map(this.mapToPost);
  }

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
      upvotes: item.upvotes ?? 0,
      downvotes: item.downvotes ?? 0,
      likes: 0,
      labelIds: item.labelIds?.map((l: any) => [l.name, l.description]) ?? [],
      topicIds: item.topicIds ?? [],
      videoImageRef: item.mediaRef ? [item.mediaRef] : [],
      sourceIds: item.sourceIds ?? [],
      comments: item.comments?.map((c: any) => ({
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
}

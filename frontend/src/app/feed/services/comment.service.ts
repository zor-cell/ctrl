import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { PostComment } from '../dto/Comment';
import { environment } from '../../../environments/environment';
import { MediaCacheService } from '../../core/services/media-cache.service';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private readonly http = inject(HttpClient);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly baseUri = environment.apiUrl;

  getComments(postId: string): Promise<PostComment[]> {
    return firstValueFrom(
      this.http.get<PostComment[]>(`${this.baseUri}/posts/${postId}/comments`)
    )
      .then(comments => this.flattenComments(comments))
      .then(comments => this.resolveCommentProfilePictures(comments));
  }

  addComment(
    postId: string,
    content: string,
    parentCommentId: string | null = null
  ): Promise<PostComment> {
    return firstValueFrom(
      this.http.post<PostComment>(`${this.baseUri}/posts/${postId}/comments`, {
        content,
        parentCommentId,
      })
    ).then(comment => this.resolveCommentProfilePicture(comment));
  }

  updateComment(commentId: string, content: string): Promise<PostComment> {
    return firstValueFrom(
      this.http.put<PostComment>(`${this.baseUri}/comments/${commentId}`, { content })
    ).then(comment => this.resolveCommentProfilePicture(comment));
  }

  deleteComment(commentId: string): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${this.baseUri}/comments/${commentId}`)
    );
  }

  private flattenComments(comments: PostComment[]): PostComment[] {
    return comments.flatMap(comment => {
      const replies = this.flattenComments(comment.replies ?? []);
      return [{ ...comment, replies: [] }, ...replies];
    });
  }

  private resolveCommentProfilePictures(comments: PostComment[]): Promise<PostComment[]> {
    return Promise.all(comments.map(comment => this.resolveCommentProfilePicture(comment)));
  }

  private async resolveCommentProfilePicture(comment: PostComment): Promise<PostComment> {
    const userProfilePic = await firstValueFrom(
      this.mediaCacheService.resolveMediaUrl(comment.userProfilePic, 'image', 'high')
    ).catch(() => comment.userProfilePic);

    return {
      ...comment,
      userProfilePic: userProfilePic ?? '',
    };
  }
}

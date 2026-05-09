import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { PostComment } from '../dto/Comment';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private readonly http = inject(HttpClient);
  private readonly baseUri = environment.apiUrl;

  getComments(postId: string): Promise<PostComment[]> {
    return firstValueFrom(
      this.http.get<PostComment[]>(`${this.baseUri}/posts/${postId}/comments`)
    );
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
    );
  }

  updateComment(commentId: string, content: string): Promise<PostComment> {
    return firstValueFrom(
      this.http.put<PostComment>(`${this.baseUri}/comments/${commentId}`, { content })
    );
  }

  deleteComment(commentId: string): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${this.baseUri}/comments/${commentId}`)
    );
  }
}

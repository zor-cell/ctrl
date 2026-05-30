import { Component, inject, signal, input, resource } from "@angular/core";
import { ProgressSpinner } from "primeng/progressspinner";
import { CommentResponse } from "../../dto/PostResponse";
import { CommentService } from "../../../feed/services/comment.service";
import { UserService } from "../../../user/user.service";
import { NotificationService } from "../../../core/services/notification.service";
import { timeAgo as formatTimeAgo } from '../../../core/utils/date-time.utils';
import { PostComment } from "../../../feed/dto/Comment";

@Component({
  selector: 'app-post-comments',
  standalone: true,
  imports: [
    ProgressSpinner,
  ],
  templateUrl: './post-comments.html',
  styleUrl: './post-comments.scss',
})
export class PostComments {
  private readonly commentService = inject(CommentService);
  private readonly userService = inject(UserService);
  private readonly notificationService = inject(NotificationService);

  protected formatTimeAgo = formatTimeAgo;
  postId = input.required<string>();
  comments = resource({
    defaultValue: [],
    loader: () => {
      return this.commentService.getComments(this.postId());
    }
  })
  loadingComments = signal<boolean>(false);
  commentDraft = signal<string>('');
  editDraft = signal<string>('');
  editingCommentId = signal<string | null>(null);
  replyingTo = signal<CommentResponse | null>(null);
  submittingComment = signal<boolean>(false);


  topLevelComments(): PostComment[] {
    return this.comments.value()?.filter(
      comment => comment.parentCommentId === null,
    );
  }

  repliesFor(commentId: string): PostComment[] {
    return this.comments.value().filter(comment => comment.parentCommentId === commentId);
  }

  commentAvatarInitial(comment: CommentResponse): string {
    return comment.username.trim().charAt(0).toUpperCase() || '?';
  }

  isOwnComment(comment: CommentResponse): boolean {
    return comment.username === this.userService.user()?.username;
  }

  // Comment actions
  setReplyingTo(comment: CommentResponse): void {
    this.replyingTo.set(comment);
    this.commentDraft.set('');
  }

  cancelReply(): void {
    this.replyingTo.set(null);
    this.commentDraft.set('');
  }

  startEdit(comment: CommentResponse): void {
    this.editingCommentId.set(comment.commentId);
    this.editDraft.set(comment.content);
  }

  updateEditDraft(event: Event): void {
    const input = event.target as HTMLTextAreaElement;
    this.editDraft.set(input.value);
  }

  cancelEdit(): void {
    this.editingCommentId.set(null);
    this.editDraft.set('');
  }

  async saveEdit(commentId: string): Promise<void> {
    const content = this.editDraft().trim();
    if (!content) {
      return;
    }

    try {
      const updated = await this.commentService.updateComment(commentId, content);
      this.comments.update(comments =>
        comments.map(comment => comment.commentId === commentId ? updated : comment),
      );
      this.cancelEdit();
    } catch (error) {
      console.error('Failed to update comment:', error);
    }
  }

  async deleteComment(commentId: string): Promise<void> {
    try {
      await this.commentService.deleteComment(commentId);
      this.comments.update(comments =>
        comments.filter(
          comment => comment.commentId !== commentId && comment.parentCommentId !== commentId,
        ),
      );
    } catch (error) {
      console.error('Failed to delete comment:', error);
    }
  }

  updateCommentDraft(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.commentDraft.set(input.value);
  }

  canSubmitComment(): boolean {
    return this.commentDraft().trim().length > 0;
  }

  async submitComment(): Promise<void> {
    const content = this.commentDraft().trim();
    if (content.length === 0 || this.submittingComment()) {
      return;
    }

    const parentCommentId = this.replyingTo()?.commentId ?? null;
    this.submittingComment.set(true);
    try {
      const created = await this.commentService.addComment(this.postId(), content, parentCommentId);
      this.comments.update(comments => [...comments, created]);
      this.commentDraft.set('');
      this.replyingTo.set(null);
    } catch (error) {
      console.error('Failed to submit comment:', error);
      this.notificationService.showError('Failed to submit comment');
    } finally {
      this.submittingComment.set(false);
    }
  }

  commentCount(): number {
    return this.comments.value().length;
  }
}

import {
  Component,
  computed,
  inject,
  Input,
  OnChanges,
  Output,
  EventEmitter,
  signal,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Post } from '../../dto/Post';
import { PostComment } from '../../dto/Comment';
import { CommentService } from '../../services/comment.service';
import { UserService } from '../../../user/user.service';

import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { TextareaModule } from 'primeng/textarea';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import {ReportComponent} from '../../../report/components/report/report.component';
import {ContentTypeEnum} from '../../../report/dto/report';
import {RouterLink} from '@angular/router';
import {FollowBadgeComponent} from '../follow-badge/follow-badge.component';

@Component({
  selector: 'app-comments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DialogModule,
    AvatarModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    TextareaModule,
    ProgressSpinnerModule,
    ReportComponent,
    RouterLink,
    FollowBadgeComponent,
  ],
  templateUrl: './comments.component.html',
  styleUrl: './comments.component.scss',
})
export class CommentsComponent implements OnChanges {
  @Input() post: Post | null = null;
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  private readonly commentService = inject(CommentService);
  private readonly userService = inject(UserService);

  // ─── State ────────────────────────────────────────────
  comments = signal<PostComment[]>([]);
  loading = signal(false);
  submitting = signal(false);

  newComment = '';
  replyingTo = signal<PostComment | null>(null);

  // Report dialog
  protected reportDialogVisible = signal(false);
  protected selectedCommentForReport = signal<PostComment | null>(null);

  editingCommentId = signal<string | null>(null);
  editContent = '';

  // ─── Derived ──────────────────────────────────────────
  topLevelComments = computed(() =>
    this.comments().filter(c => c.parentCommentId === null)
  );

  repliesFor(commentId: string): PostComment[] {
    return this.comments().filter(c => c.parentCommentId === commentId);
  }

  closeDialog(): void {
    this.visibleChange.emit(false);
  }

  isOwnComment(comment: PostComment): boolean {
    // Adjust `.user()?.username` to match your actual UserService API
    return comment.username === this.userService.user()?.username;
  }

  // ─── Lifecycle ────────────────────────────────────────
  ngOnChanges(changes: SimpleChanges): void {
    const visibleChange = changes['visible'];
    const becameVisible =
      visibleChange && !visibleChange.previousValue && visibleChange.currentValue;

    const postChange = changes['post'];
    const postChanged =
      postChange &&
      postChange.currentValue?.postId !== postChange.previousValue?.postId;

    if ((becameVisible || postChanged) && this.post && this.visible) {
      this.loadComments();
    }

    // Reset state when dialog closes
    if (visibleChange && !visibleChange.currentValue) {
      this.cancelEdit();
      this.cancelReply();
      this.newComment = '';
    }
  }

  // ─── Load ─────────────────────────────────────────────
  private async loadComments(): Promise<void> {
    if (!this.post) return;
    this.loading.set(true);
    try {
      const loaded = await this.commentService.getComments(this.post.postId);
      this.setComments(loaded);
    } catch (err) {
      console.error('Failed to load comments:', err);
    } finally {
      this.loading.set(false);
    }
  }

  // ─── Submit ───────────────────────────────────────────
  async submitComment(): Promise<void> {
    const text = this.newComment.trim();
    if (!text || !this.post || this.submitting()) return;

    this.submitting.set(true);
    try {
      const created = await this.commentService.addComment(
        this.post.postId,
        text,
        this.replyingTo()?.commentId ?? null
      );
      this.setComments([...this.comments(), created]);
      this.newComment = '';
      this.replyingTo.set(null);
    } catch (err) {
      console.error('Failed to submit comment:', err);
    } finally {
      this.submitting.set(false);
    }
  }

  // ─── Edit ─────────────────────────────────────────────
  startEdit(comment: PostComment): void {
    this.editingCommentId.set(comment.commentId);
    this.editContent = comment.content;
  }

  cancelEdit(): void {
    this.editingCommentId.set(null);
    this.editContent = '';
  }

  async saveEdit(commentId: string): Promise<void> {
    const text = this.editContent.trim();
    if (!text) return;

    try {
      const updated = await this.commentService.updateComment(commentId, text);
      this.setComments(
        this.comments().map(c => (c.commentId === commentId ? updated : c))
      );
      this.cancelEdit();
    } catch (err) {
      console.error('Failed to update comment:', err);
    }
  }

  // ─── Delete ───────────────────────────────────────────
  async deleteComment(commentId: string): Promise<void> {
    if (!confirm('Delete this comment?')) return;
    try {
      await this.commentService.deleteComment(commentId);
      // Remove comment and all its replies
      this.setComments(
        this.comments().filter(
          c => c.commentId !== commentId && c.parentCommentId !== commentId
        )
      );
    } catch (err) {
      console.error('Failed to delete comment:', err);
    }
  }

  // ─── Reply ────────────────────────────────────────────
  setReplyingTo(comment: PostComment): void {
    this.replyingTo.set(comment);
    this.newComment = '';
  }

  cancelReply(): void {
    this.replyingTo.set(null);
    this.newComment = '';
  }

  // ─── Helpers ──────────────────────────────────────────

  /** Syncs local signal + post.comments (keeps feed comment count in sync).
   *  post.comments is deferred to avoid ExpressionChangedAfterItHasBeenCheckedError
   *  since FeedComponent reads post.comments.length in the same cycle. */
  private setComments(comments: PostComment[]): void {
    this.comments.set(comments);
    setTimeout(() => {
      if (this.post) this.post.comments = comments;
    }, 0);
  }

  timeAgo(createdAt: string): string {
    const normalized = createdAt.endsWith('Z') ? createdAt : createdAt + 'Z';
    const diffMs = Date.now() - new Date(normalized).getTime();
    const minutes = Math.floor(diffMs / 60_000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  }

  isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)(?:[?#].*)?$/i.test(url);
  }

  reportComment(comment: PostComment) {
    this.selectedCommentForReport.set(comment);
    this.reportDialogVisible.set(true);
  }

  protected readonly ContentTypeEnum = ContentTypeEnum;
}

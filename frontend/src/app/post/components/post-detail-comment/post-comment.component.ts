import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  signal,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { DialogModule } from 'primeng/dialog';

import { CommentResponse } from '../../dto/PostResponse';
import { CommentService } from '../../../feed/services/comment.service';
import { UserService } from '../../../user/user.service';
import { ReportComponent } from '../../../report/components/report/report.component';
import { ContentTypeEnum } from '../../../report/dto/report';

@Component({
  selector: 'app-post-comment',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    AvatarModule,
    ButtonModule,
    TextareaModule,
    ProgressSpinnerModule,
    ReportComponent,
    DialogModule,
  ],
  templateUrl: './post-comment.component.html',
  styleUrl: './post-comment.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostCommentComponent implements OnChanges {
  private readonly commentService = inject(CommentService);
  private readonly userService    = inject(UserService);
  private readonly cdr            = inject(ChangeDetectorRef);

  @Input() postId: string | null = null;
  @Output() readonly commentAdded = new EventEmitter<void>();

  protected readonly comments         = signal<CommentResponse[]>([]);
  protected readonly loadingComments  = signal(false);
  protected readonly submitting       = signal(false);
  protected readonly replyingTo       = signal<CommentResponse | null>(null);
  protected readonly editingCommentId = signal<string | null>(null);
  protected newComment  = '';
  protected editContent = '';

  // ─── Report ───────────────────────────────────────────
  protected readonly reportCommentVisible     = signal(false);
  protected readonly selectedCommentForReport = signal<CommentResponse | null>(null);
  protected readonly ContentTypeEnum          = ContentTypeEnum;

  // ─── Delete confirm ───────────────────────────────────
  protected readonly deleteConfirmVisible    = signal(false);
  protected readonly deleteCommentInProgress = signal(false);
  private pendingDeleteCommentId: string | null = null;

  /** Two-way binding shim for p-dialog [(visible)] */
  protected get deleteConfirmVisibleModel(): boolean { return this.deleteConfirmVisible(); }
  protected set deleteConfirmVisibleModel(v: boolean) { this.deleteConfirmVisible.set(v); }

  // ─── Lifecycle ────────────────────────────────────────

  ngOnChanges(changes: SimpleChanges): void {
    const change   = changes['postId'];
    const current  = change?.currentValue as string | null;
    const previous = change?.previousValue as string | null;

    if (current && current !== previous) {
      this.reset();
      this.fetchComments(current);
    }
    if (!current) {
      this.reset();
    }
  }

  // ─── Data fetching ────────────────────────────────────

  async fetchComments(postId: string): Promise<void> {
    this.loadingComments.set(true);
    try {
      const comments = await this.commentService.getComments(postId);
      this.comments.set(comments);
    } catch {
      this.comments.set([]);
    } finally {
      this.loadingComments.set(false);
      this.cdr.markForCheck();
    }
  }

  /** Expose comments count for parent (e.g. action bar badge). */
  get count(): number {
    return this.comments().length;
  }

  private reset(): void {
    this.comments.set([]);
    this.replyingTo.set(null);
    this.editingCommentId.set(null);
    this.newComment  = '';
    this.editContent = '';
  }

  // ─── Derived lists ────────────────────────────────────

  protected topLevelComments(): CommentResponse[] {
    return this.comments().filter(c => c.parentCommentId === null);
  }

  protected repliesFor(commentId: string): CommentResponse[] {
    return this.comments().filter(c => c.parentCommentId === commentId);
  }

  // ─── Ownership ────────────────────────────────────────

  protected isOwnComment(comment: CommentResponse): boolean {
    return comment.username === this.userService.user()?.username;
  }

  // ─── Submit ───────────────────────────────────────────

  protected async submitComment(): Promise<void> {
    const text = this.newComment.trim();
    if (!text || !this.postId || this.submitting()) return;
    this.submitting.set(true);
    try {
      const created = await this.commentService.addComment(
        this.postId,
        text,
        this.replyingTo()?.commentId ?? null,
      );
      this.comments.update(c => [...c, created]);
      this.newComment = '';
      this.replyingTo.set(null);
      this.commentAdded.emit();
      this.cdr.markForCheck();
    } catch (err) {
      console.error('Failed to submit comment:', err);
    } finally {
      this.submitting.set(false);
    }
  }

  // ─── Edit ─────────────────────────────────────────────

  protected startEdit(comment: CommentResponse): void {
    this.editingCommentId.set(comment.commentId);
    this.editContent = comment.content;
  }

  protected cancelEdit(): void {
    this.editingCommentId.set(null);
    this.editContent = '';
  }

  protected async saveEdit(commentId: string): Promise<void> {
    const text = this.editContent.trim();
    if (!text) return;
    try {
      const updated = await this.commentService.updateComment(commentId, text);
      this.comments.update(c => c.map(x => x.commentId === commentId ? updated : x));
      this.cancelEdit();
      this.cdr.markForCheck();
    } catch (err) {
      console.error('Failed to update comment:', err);
    }
  }

  // ─── Delete ───────────────────────────────────────────

  protected openDeleteConfirm(commentId: string): void {
    this.pendingDeleteCommentId = commentId;
    this.deleteConfirmVisible.set(true);
  }

  protected async confirmDeleteComment(): Promise<void> {
    const commentId = this.pendingDeleteCommentId;
    if (!commentId || this.deleteCommentInProgress()) return;
    this.deleteCommentInProgress.set(true);
    try {
      await this.commentService.deleteComment(commentId);
      this.comments.update(c =>
        c.filter(x => x.commentId !== commentId && x.parentCommentId !== commentId),
      );
      this.deleteConfirmVisible.set(false);
      this.pendingDeleteCommentId = null;
      this.cdr.markForCheck();
    } catch (err) {
      console.error('Failed to delete comment:', err);
    } finally {
      this.deleteCommentInProgress.set(false);
      this.cdr.markForCheck();
    }
  }

  // ─── Reply ────────────────────────────────────────────

  protected setReplyingTo(comment: CommentResponse): void {
    this.replyingTo.set(comment);
    this.newComment = '';
  }

  protected cancelReply(): void {
    this.replyingTo.set(null);
    this.newComment = '';
  }

  // ─── Report ───────────────────────────────────────────

  protected onReportComment(comment: CommentResponse): void {
    this.selectedCommentForReport.set(comment);
    this.reportCommentVisible.set(true);
  }

  // ─── Utilities ────────────────────────────────────────

  protected timeAgo(createdAt: string): string {
    const normalized = createdAt.endsWith('Z') ? createdAt : createdAt + 'Z';
    const diffMs  = Date.now() - new Date(normalized).getTime();
    const minutes = Math.floor(diffMs / 60_000);
    if (minutes < 1)  return 'just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24)   return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
  }
}

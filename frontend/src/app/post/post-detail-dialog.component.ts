import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
  signal,
} from '@angular/core';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';

import { timeAgo as formatTimeAgo } from '../core/utils/date-time.utils';
import { usernameInitial } from '../core/utils/username-initial.utils';
import { CommentService } from '../feed/services/comment.service';
import { UserService } from '../user/user.service';
import { CommentResponse, PostResponse, SingleSourceResponse } from './dto/PostResponse';
import { PostCaptionFormatPipe } from './post-caption-format.pipe';
import { postMediaKind } from './post-media.utils';
import { PreviewKind, TopicOption } from './post-page.models';

@Component({
  selector: 'app-post-detail-dialog',
  imports: [CommonModule, Button, Dialog, PostCaptionFormatPipe],
  templateUrl: './post-detail-dialog.component.html',
  styleUrl: './post-detail-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostDetailDialogComponent implements OnChanges {
  private readonly commentService = inject(CommentService);
  private readonly userService = inject(UserService);

  @Input() post: PostResponse | null = null;
  @Input() topicOptions: TopicOption[] = [];
  @Input() currentProfilePicture: string | null = null;
  @Input() avatarInitial = '?';
  @Input() canDelete = true;
  @Input() resolvedMediaUrl: string | null = null;

  @Output() readonly dismissed = new EventEmitter<void>();
  @Output() readonly deleteRequested = new EventEmitter<PostResponse>();

  protected readonly commentDraft = signal('');
  protected readonly localComments = signal<CommentResponse[]>([]);
  protected readonly loadingComments = signal(false);
  protected readonly submittingComment = signal(false);
  protected readonly replyingTo = signal<CommentResponse | null>(null);
  protected readonly editingCommentId = signal<string | null>(null);
  protected readonly editDraft = signal('');
  protected readonly sourcesExpanded = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    const postChange = changes['post'];
    const previousPostId = postChange?.previousValue?.postId;
    const currentPostId = postChange?.currentValue?.postId;

    if (currentPostId && currentPostId !== previousPostId) {
      this.localComments.set([]);
      this.loadComments(currentPostId);
    }
  }

  protected onVisibleChange(visible: boolean) {
    if (!visible) {
      this.closeDialog();
    }
  }

  protected closeDialog() {
    this.commentDraft.set('');
    this.localComments.set([]);
    this.replyingTo.set(null);
    this.cancelEdit();
    this.sourcesExpanded.set(false);
    this.dismissed.emit();
  }

  protected requestDeletion() {
    if (this.post) {
      this.deleteRequested.emit(this.post);
    }
  }

  protected updateCommentDraft(event: Event) {
    const input = event.target as HTMLInputElement;
    this.commentDraft.set(input.value);
  }

  protected canSubmitComment(): boolean {
    return this.commentDraft().trim().length > 0;
  }

  protected async submitComment() {
    const content = this.commentDraft().trim();
    if (!this.post || content.length === 0 || this.submittingComment()) {
      return;
    }

    const parentCommentId = this.replyingTo()?.commentId ?? null;
    this.submittingComment.set(true);
    try {
      const created = await this.commentService.addComment(this.post.postId, content, parentCommentId);
      this.localComments.update(comments => [...comments, created]);
      this.commentDraft.set('');
      this.replyingTo.set(null);
    } catch (error) {
      console.error('Failed to submit comment:', error);
    } finally {
      this.submittingComment.set(false);
    }
  }

  protected commentCount(): number {
    return this.localComments().length;
  }

  protected postMediaKind(post: PostResponse): PreviewKind {
    return postMediaKind(post.mediaRef);
  }

  protected postMediaUrl(): string | null {
    return this.resolvedMediaUrl;
  }

  protected postSources(post: PostResponse): SingleSourceResponse[] {
    return post.source?.singleSources ?? [];
  }

  protected toggleSources() {
    this.sourcesExpanded.update(expanded => !expanded);
  }

  protected sourcesButtonLabel(post: PostResponse): string {
    const sourceCount = this.postSources(post).length;
    const sourceLabel = sourceCount === 1 ? 'source' : 'sources';
    return this.sourcesExpanded()
      ? 'Hide sources'
      : `Sources (${sourceCount} ${sourceLabel})`;
  }

  protected topicLabelById(topicId: string): string {
    return this.topicOptions.find(option => option.value === topicId)?.label ?? topicId;
  }

  protected displaySourceUrl(url: string): string {
    const maxLength = 120;
    return url.length <= maxLength ? url : `${url.slice(0, maxLength - 1)}...`;
  }

  protected formattedSourceDate(source: SingleSourceResponse): string {
    if (!source.date) {
      return '';
    }

    return new Intl.DateTimeFormat('de-AT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(new Date(source.date));
  }

  protected timeAgo(value: string): string {
    return formatTimeAgo(value);
  }

  protected topLevelComments(): CommentResponse[] {
    return this.localComments().filter(
      comment => comment.parentCommentId === null,
    );
  }

  protected repliesFor(commentId: string): CommentResponse[] {
    return this.localComments().filter(comment => comment.parentCommentId === commentId);
  }

  protected commentAvatarInitial(comment: CommentResponse): string {
    return usernameInitial(comment.username);
  }

  protected isOwnComment(comment: CommentResponse): boolean {
    return comment.username === this.userService.user()?.username;
  }

  protected setReplyingTo(comment: CommentResponse): void {
    this.replyingTo.set(comment);
    this.commentDraft.set('');
  }

  protected cancelReply(): void {
    this.replyingTo.set(null);
    this.commentDraft.set('');
  }

  protected startEdit(comment: CommentResponse): void {
    this.editingCommentId.set(comment.commentId);
    this.editDraft.set(comment.content);
  }

  protected updateEditDraft(event: Event): void {
    const input = event.target as HTMLTextAreaElement;
    this.editDraft.set(input.value);
  }

  protected cancelEdit(): void {
    this.editingCommentId.set(null);
    this.editDraft.set('');
  }

  protected async saveEdit(commentId: string): Promise<void> {
    const content = this.editDraft().trim();
    if (!content) {
      return;
    }

    try {
      const updated = await this.commentService.updateComment(commentId, content);
      this.localComments.update(comments =>
        comments.map(comment => comment.commentId === commentId ? updated : comment),
      );
      this.cancelEdit();
    } catch (error) {
      console.error('Failed to update comment:', error);
    }
  }

  protected async deleteComment(commentId: string): Promise<void> {
    try {
      await this.commentService.deleteComment(commentId);
      this.localComments.update(comments =>
        comments.filter(
          comment => comment.commentId !== commentId && comment.parentCommentId !== commentId,
        ),
      );
    } catch (error) {
      console.error('Failed to delete comment:', error);
    }
  }

  private async loadComments(postId: string): Promise<void> {
    this.loadingComments.set(true);
    try {
      const comments = await this.commentService.getComments(postId);
      this.localComments.set(comments);
    } catch (error) {
      console.error('Failed to load comments:', error);
      this.localComments.set([]);
    } finally {
      this.loadingComments.set(false);
    }
  }
}

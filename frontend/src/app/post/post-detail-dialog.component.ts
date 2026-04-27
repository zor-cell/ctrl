import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  signal,
} from '@angular/core';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';

import { timeAgo as formatTimeAgo } from '../core/utils/date-time.utils';
import { CommentResponse, PostResponse, SingleSourceResponse } from './dto/PostResponse';
import { postMediaKind } from './post-media.utils';
import { PreviewKind, TopicOption } from './post-page.models';

@Component({
  selector: 'app-post-detail-dialog',
  imports: [CommonModule, Button, Dialog],
  templateUrl: './post-detail-dialog.component.html',
  styleUrl: './post-detail-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostDetailDialogComponent {
  @Input() post: PostResponse | null = null;
  @Input() topicOptions: TopicOption[] = [];
  @Input() currentProfilePicture: string | null = null;
  @Input() avatarInitial = '?';

  @Output() readonly dismissed = new EventEmitter<void>();
  @Output() readonly deleteRequested = new EventEmitter<PostResponse>();

  protected readonly commentDraft = signal('');
  protected readonly localComments = signal<CommentResponse[]>([]);
  protected readonly sourcesExpanded = signal(false);

  protected onVisibleChange(visible: boolean) {
    if (!visible) {
      this.closeDialog();
    }
  }

  protected closeDialog() {
    this.commentDraft.set('');
    this.localComments.set([]);
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

  protected submitComment() {
    const content = this.commentDraft().trim();
    if (!this.post || content.length === 0) {
      return;
    }

    this.localComments.update(comments => [
      ...comments,
      {
        commentId: `local-${Date.now()}`,
        username: this.post?.username ?? '',
        content,
        createdAt: new Date().toISOString(),
        parentCommentId: null,
      },
    ]);
    this.commentDraft.set('');
  }

  protected commentCount(): number {
    return (this.post?.comments.length ?? 0) + this.localComments().length;
  }

  protected postMediaKind(post: PostResponse): PreviewKind {
    return postMediaKind(post.mediaRef ?? post.mediaUrl);
  }

  protected postMediaUrl(post: PostResponse): string | null {
    return post.mediaUrl;
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

  protected topLevelComments(post: PostResponse): CommentResponse[] {
    return [...post.comments, ...this.localComments()].filter(
      comment => comment.parentCommentId === null,
    );
  }
}

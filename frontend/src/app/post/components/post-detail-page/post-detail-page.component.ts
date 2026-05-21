import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';

import { timeAgo as formatTimeAgo } from '../../../core/utils/date-time.utils';
import { CommentService } from '../../../feed/services/comment.service';
import { UserService } from '../../../user/user.service';
import { PostService } from '../../post.service';
import { PostResponse, CommentResponse, SingleSourceResponse } from '../../dto/PostResponse';
import { postMediaKind } from '../../post-media.utils';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-post-detail-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    AvatarModule,
    CardModule,
    ProgressSpinnerModule,
    TooltipModule,
  ],
  templateUrl: './post-detail-page.component.html',
  styleUrl: './post-detail-page.component.scss',
})
export class PostDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly commentService = inject(CommentService);
  private readonly userService = inject(UserService);
  private readonly postService = inject(PostService);
  private readonly notificationService = inject(NotificationService);

  // Post data
  post = signal<PostResponse | null>(null);
  isLoading = signal<boolean>(true);
  error = signal<string | null>(null);

  // Comments state
  localComments = signal<CommentResponse[]>([]);
  loadingComments = signal<boolean>(false);
  commentDraft = signal<string>('');
  submittingComment = signal<boolean>(false);
  replyingTo = signal<CommentResponse | null>(null);
  editingCommentId = signal<string | null>(null);
  editDraft = signal<string>('');
  sourcesExpanded = signal<boolean>(false);

  ngOnInit(): void {
    const postId = this.route.snapshot.paramMap.get('postId');
    if (!postId) {
      this.error.set('No post ID provided');
      this.isLoading.set(false);
      return;
    }

    this.loadPost(postId);
  }

  private async loadPost(postId: string): Promise<void> {
    this.isLoading.set(true);
    try {
      const post = await this.postService.getPostById(postId);
      this.post.set(post);
      this.loadComments(postId);
    } catch (error) {
      this.error.set('Failed to load post');
      console.error('Failed to load post:', error);
    } finally {
      this.isLoading.set(false);
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

  goBack(): void {
    this.location.back();
  }

  // Helper methods
  postMediaKind(post: PostResponse): 'image' | 'video' | null {
    return postMediaKind(post.mediaRef ?? post.mediaUrl);
  }

  postMediaUrl(post: PostResponse): string | null {
    return post.mediaUrl;
  }

  postSources(post: PostResponse): SingleSourceResponse[] {
    return post.source?.singleSources ?? [];
  }

  toggleSources(): void {
    this.sourcesExpanded.update(expanded => !expanded);
  }

  sourcesButtonLabel(post: PostResponse): string {
    const sourceCount = this.postSources(post).length;
    const sourceLabel = sourceCount === 1 ? 'source' : 'sources';
    return this.sourcesExpanded()
      ? 'Hide sources'
      : `Sources (${sourceCount} ${sourceLabel})`;
  }

  displaySourceUrl(url: string): string {
    const maxLength = 120;
    return url.length <= maxLength ? url : `${url.slice(0, maxLength - 1)}...`;
  }

  formattedSourceDate(source: SingleSourceResponse): string {
    if (!source.date) {
      return '';
    }
    return new Intl.DateTimeFormat('de-AT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(new Date(source.date));
  }

  timeAgo(value: string): string {
    return formatTimeAgo(value);
  }

  topLevelComments(): CommentResponse[] {
    return this.localComments().filter(
      comment => comment.parentCommentId === null,
    );
  }

  repliesFor(commentId: string): CommentResponse[] {
    return this.localComments().filter(comment => comment.parentCommentId === commentId);
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
      this.localComments.update(comments =>
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
      this.localComments.update(comments =>
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
    const post = this.post();
    if (!post || content.length === 0 || this.submittingComment()) {
      return;
    }

    const parentCommentId = this.replyingTo()?.commentId ?? null;
    this.submittingComment.set(true);
    try {
      const created = await this.commentService.addComment(post.postId, content, parentCommentId);
      this.localComments.update(comments => [...comments, created]);
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
    return this.localComments().length;
  }
}

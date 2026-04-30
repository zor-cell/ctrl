import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Post } from '../../dto/Post';
import { PostComment } from '../../dto/Comment';
import { timeAgo as formatTimeAgo } from '../../../core/utils/date-time.utils';

import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { TextareaModule } from 'primeng/textarea';
import {ReportComponent} from '../../../report/components/report/report.component';
import {ContentTypeEnum} from '../../../report/dto/report';

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
    ReportComponent,
  ],
  templateUrl: './comments.component.html',
  styleUrl: './comments.component.scss',
})
export class CommentsComponent {
  @Input() post: Post | null = null;
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  newComment = '';
  replyingTo = signal<PostComment | null>(null);

  // Report dialog
  protected reportDialogVisible = signal(false);
  protected selectedCommentForReport = signal<PostComment | null>(null);

  close(): void {
    this.visibleChange.emit(false);
  }

  topLevelComments(): PostComment[] {
    return this.post?.comments.filter((c) => c.parentCommentId === null) ?? [];
  }

  repliesFor(commentId: string): PostComment[] {
    return this.post?.comments.filter((c) => c.parentCommentId === commentId) ?? [];
  }

  setReplyingTo(comment: PostComment): void {
    this.replyingTo.set(comment);
    this.newComment = '';
  }

  cancelReply(): void {
    this.replyingTo.set(null);
    this.newComment = '';
  }

  submitComment(): void {
    const text = this.newComment.trim();
    if (!text || !this.post) return;

    const comment: PostComment = {
      commentId: 'c-' + Date.now(),
      userId: 'me',
      username: 'me',
      userProfilePic: '',
      createdAt: new Date().toISOString(),
      content: text,
      parentCommentId: this.replyingTo()?.commentId ?? null,
    };

    this.post.comments.push(comment);
    this.newComment = '';
    this.replyingTo.set(null);
  }

  timeAgo(createdAt: string): string {
    return formatTimeAgo(createdAt);
  }

  isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)$/i.test(url);
  }

  reportComment(comment: PostComment) {
    this.selectedCommentForReport.set(comment);
    this.reportDialogVisible.set(true);
  }

  protected readonly ContentTypeEnum = ContentTypeEnum;
}

import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Post } from '../dto/Post';
import { PostComment } from '../dto/Comment';
import { FeedComponent } from '../feed.component';

import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-comments',
  standalone: true,
  imports: [
    CommonModule,
    DialogModule,
    AvatarModule,
    ButtonModule,
    TagModule,
  ],
  templateUrl: './comments.component.html',
  styleUrl: './comments.component.scss',
})
export class CommentsComponent {
  @Input() post: Post | null = null;
  @Input() visible: boolean = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  close(): void {
    this.visibleChange.emit(false);
  }

  /** Returns only top-level comments (no parent). */
  topLevelComments(): PostComment[] {
    return this.post?.comments.filter(c => c.parentCommentId === null) ?? [];
  }

  /** Returns direct replies for a given comment. */
  repliesFor(commentId: string): PostComment[] {
    return this.post?.comments.filter(c => c.parentCommentId === commentId) ?? [];
  }

  timeAgo(createdAt: string): string {
    const diffMs = Date.now() - new Date(createdAt).getTime();
    const minutes = Math.floor(diffMs / 60_000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  }
}

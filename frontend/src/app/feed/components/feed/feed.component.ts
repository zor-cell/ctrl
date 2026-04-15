import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { UserService } from '../../../user/user.service';
import { FeedService } from '../../services/feed.service';
import { Post } from '../../dto/Post';
import { CommentsComponent } from '../comments/comments.component';

import { TabsModule } from 'primeng/tabs';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [
    CommonModule,
    TabsModule,
    CardModule,
    ButtonModule,
    DialogModule,
    AvatarModule,
    TagModule,
    TooltipModule,
    CommentsComponent,
  ],
  templateUrl: './feed.component.html',
  styleUrl: './feed.component.scss',
})
export class FeedComponent implements OnInit {
  protected userService = inject(UserService);
  private readonly feedService = inject(FeedService);

  protected allPosts = signal<Post[]>([]);

  // Why dialog
  protected whyDialogVisible = signal(false);
  protected selectedPost = signal<Post | null>(null);

  // Comments dialog
  protected commentsDialogVisible = signal(false);
  protected selectedPostForComments = signal<Post | null>(null);

  protected publicPosts = computed(() => this.allPosts().filter(p => p.isPublic));
  protected privatePosts = computed(() => this.allPosts().filter(p => !p.isPublic));

  async ngOnInit() {
    this.userService.loadUser();
    const posts = await this.feedService.getFeed();
    console.log(posts);
    this.allPosts.set(posts.sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    ));
  }

  getDomain(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
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

  openWhyDialog(post: Post): void {
    this.selectedPost.set(post);
    this.whyDialogVisible.set(true);
  }

  closeWhyDialog(): void {
    this.whyDialogVisible.set(false);
    this.selectedPost.set(null);
  }

  openCommentsDialog(post: Post): void {
    this.selectedPostForComments.set(post);
    this.commentsDialogVisible.set(true);
  }

  isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)$/i.test(url);
  }
}

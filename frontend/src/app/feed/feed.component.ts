import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { UserService } from '../user/user.service';
import { FeedService } from './feed.service';
import { Post } from './dto/Post';

import { TabsModule } from 'primeng/tabs';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
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
  ],
  templateUrl: './feed.component.html',
  styleUrl: './feed.component.scss',
})
export class FeedComponent implements OnInit {
  protected userService = inject(UserService);
  private feedService = inject(FeedService);

  protected allPosts = signal<Post[]>([]);
  protected whyDialogVisible = signal(false);
  protected selectedPost = signal<Post | null>(null);

  protected publicPosts = computed(() => this.allPosts().filter(p => p.isPublic));
  protected privatePosts = computed(() => this.allPosts().filter(p => !p.isPublic));

  ngOnInit() {
    this.userService.loadUser();
    this.allPosts.set(this.feedService.getMockPosts());
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
}

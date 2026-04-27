import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { UserService } from '../../../user/user.service';
import { FeedService } from '../../services/feed.service';
import { Post } from '../../dto/Post';
import { PostSource } from '../../dto/PostSource';
import { CommentsComponent } from '../comments/comments.component';

import { TabsModule } from 'primeng/tabs';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { CommonModule } from '@angular/common';
import { PostService, VoteType } from '../../../post/post.service';
import { timeAgo as formatTimeAgo } from '../../../core/utils/date-time.utils';

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
  postService = inject(PostService);

  protected allPosts = signal<Post[]>([]);

  // Why dialog
  protected whyDialogVisible = signal(false);
  protected selectedPost = signal<Post | null>(null);

  // Comments dialog
  protected commentsDialogVisible = signal(false);
  protected selectedPostForComments = signal<Post | null>(null);

  // Prevents double-clicks whilst a request is in progress
  private readonly votingInProgress = new Set<string>();
  private readonly sourceVotingInProgress = new Set<string>();

  protected publicPosts = computed(() => this.allPosts().filter(p => p.isPublic));
  protected privatePosts = computed(() => this.allPosts().filter(p => !p.isPublic));

  async ngOnInit() {
    this.userService.loadUser();
    const posts = await this.feedService.getFeed();
    console.log(posts);
    this.allPosts.set(posts);
  }

  async votePost(post: Post, voteType: VoteType): Promise<void> {
    if (this.votingInProgress.has(post.postId)) return;
    this.votingInProgress.add(post.postId);

    const previousVote = post.userVote;
    const previousUpvotes = post.upvotes;
    const previousDownvotes = post.downvotes;

    const isSameVote = post.userVote === voteType;
    const optimistic = this.calcOptimisticVote(post.upvotes, post.downvotes, post.userVote, voteType, isSameVote);
    this.patchPost(post.postId, optimistic);

    try {
      const result = await this.postService.votePost(post.postId, voteType);
      this.patchPost(post.postId, {
        upvotes: result.upvotes,
        downvotes: result.downvotes,
        userVote: result.userVote,
      });
    } catch (err) {
      console.error('Post-Vote fehlgeschlagen, wird zurückgesetzt:', err);
      this.patchPost(post.postId, {
        upvotes: previousUpvotes,
        downvotes: previousDownvotes,
        userVote: previousVote,
      });
    } finally {
      this.votingInProgress.delete(post.postId);
    }
  }

  async voteSource(post: Post, voteType: VoteType): Promise<void> {
    const source: PostSource | undefined = post.sourceIds?.[0];
    if (!source?.sourceId) return;
    if (this.sourceVotingInProgress.has(source.sourceId)) return;
    this.sourceVotingInProgress.add(source.sourceId);

    const previousVote = source.userVote;
    const previousUpvotes = source.upvotes;
    const previousDownvotes = source.downvotes;

    const isSameVote = source.userVote === voteType;
    const optimistic = this.calcOptimisticVote(source.upvotes, source.downvotes, source.userVote, voteType, isSameVote);
    this.patchSource(post.postId, optimistic);

    try {
      const result = await this.postService.voteSource(source.sourceId, voteType);
      this.patchSource(post.postId, {
        upvotes: result.upvotes,
        downvotes: result.downvotes,
        userVote: result.userVote,
      });
    } catch (err) {
      console.error('Source vote failed; will be reset:', err);
      this.patchSource(post.postId, {
        upvotes: previousUpvotes,
        downvotes: previousDownvotes,
        userVote: previousVote,
      });
    } finally {
      this.sourceVotingInProgress.delete(source.sourceId);
    }
  }

  /** Helpers **/

  private calcOptimisticVote(
    upvotes: number,
    downvotes: number,
    currentVote: 'up' | 'down' | null | undefined,
    voteType: VoteType,
    isSameVote: boolean
  ): { upvotes: number; downvotes: number; userVote: VoteType | null } {
    if (isSameVote) {
      if (voteType === 'up') upvotes = Math.max(0, upvotes - 1);
      else downvotes = Math.max(0, downvotes - 1);
      return { upvotes, downvotes, userVote: null };
    }
    if (voteType === 'up') {
      upvotes += 1;
      if (currentVote === 'down') downvotes = Math.max(0, downvotes - 1);
    } else {
      downvotes += 1;
      if (currentVote === 'up') upvotes = Math.max(0, upvotes - 1);
    }
    return { upvotes, downvotes, userVote: voteType };
  }

  private patchPost(postId: string, patch: Partial<Post>): void {
    this.allPosts.update(posts =>
      posts.map(p => (p.postId === postId ? { ...p, ...patch } : p))
    );
  }

  private patchSource(postId: string, patch: Partial<PostSource>): void {
    this.allPosts.update(posts =>
      posts.map(p => {
        if (p.postId !== postId || !p.sourceIds?.length) return p;
        const updatedSources: PostSource[] = p.sourceIds.map((s, i) =>
          i === 0 ? { ...s, ...patch } : s
        );
        return { ...p, sourceIds: updatedSources };
      })
    );
  }

  getDomain(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
  }

  timeAgo(createdAt: string): string {
    return formatTimeAgo(createdAt);
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

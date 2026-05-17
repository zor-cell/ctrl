import { Component, inject, OnInit, signal, ElementRef } from '@angular/core';
import { forkJoin } from 'rxjs';
import { UserService } from '../../../user/user.service';
import { FeedService } from '../../services/feed.service';
import { Post } from '../../dto/Post';
import { PostSource } from '../../dto/PostSource';
import { CommentsComponent } from '../comments/comments.component';
import { AlgorithmConfigComponent } from '../../../algorithm/components/config/config.component';

import { TabsModule } from 'primeng/tabs';
import { ReportComponent } from '../../../report/components/report/report.component';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { CommonModule } from '@angular/common';
import { PostService, VoteType } from '../../../post/post.service';
import {ContentTypeEnum} from '../../../report/dto/report';
import {ViewTrackingService} from '../../../post/services/view-tracking.service';
import {PostViewDirective, ViewEvent} from '../../../post/post-view-directive/post-view.directive';
import {ReactiveFormsModule} from "@angular/forms";
import {RouterLink} from "@angular/router";

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
    AlgorithmConfigComponent,
    PostViewDirective,
    ReactiveFormsModule,
    RouterLink,
    ReportComponent,
  ],
  templateUrl: './feed.component.html',
  styleUrl: './feed.component.scss',
})
export class FeedComponent implements OnInit {
  protected userService = inject(UserService);
  private readonly feedService = inject(FeedService);
  postService = inject(PostService);

  readonly viewTracking = inject(ViewTrackingService);

  protected whyDialogVisible = signal(false);
  protected selectedPost = signal<Post | null>(null);

  protected commentsDialogVisible = signal(false);
  protected selectedPostForComments = signal<Post | null>(null);

  // Report dialog
  protected reportDialogVisible = signal(false);
  protected selectedPostForReport = signal<Post | null>(null);

  //Report source
  protected reportSourceDialogVisible = signal(false);
  protected selectedSourceForReport = signal<PostSource | null>(null);
  protected selectedPostForSourceReport = signal<Post | null>(null);


  protected algorithmConfigVisible = signal(false);
  protected isRefreshing = signal(false);
  protected isSpinning = signal(false);

  protected activeTab = signal<string>(
    localStorage.getItem('feed-active-tab') ?? 'private'
  );
  protected onTabChange(tab: string | number | undefined): void {
    if (tab == null) return;
    const value = String(tab);
    this.activeTab.set(value);
    localStorage.setItem('feed-active-tab', value);
  }


  private readonly elRef = inject(ElementRef);

  // Prevents double-clicks whilst a request is in progress
  private readonly votingInProgress = new Set<string>();
  private readonly sourceVotingInProgress = new Set<string>();

  protected publicPosts = signal<Post[]>([]);
  protected privatePosts = signal<Post[]>([]);

  ngOnInit(): void {
    this.userService.loadUser();
    this.loadFeed();
  }

  //openAlgorithmConfig(): void {
  //  this.algorithmConfigVisible.set(true);
  //}

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
      console.error('Post-vote failed; will be reset:', err);
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

  getDomain(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
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
    return /\.(mp4|webm|ogg|mov)(?:[?#].*)?$/i.test(url);
  }

  onPostViewed(event: ViewEvent): void {
    this.viewTracking.track(event);
  }

  /** Helpers **/
  protected loadFeed() {
    this.isRefreshing.set(true);
    this.isSpinning.set(true);
    setTimeout(() => this.isSpinning.set(false), 600);

    forkJoin([
      this.feedService.getFeed(true),
      this.feedService.getFeed(false),
    ]).subscribe(([publicPosts, privatePosts]) => {
      this.publicPosts.set(publicPosts);
      this.privatePosts.set(privatePosts);
      this.isRefreshing.set(false);
      this.scrollFeedToTop();
    });
  }

  private scrollFeedToTop(): void {
    const lists = this.elRef.nativeElement.querySelectorAll('.posts-list') as NodeListOf<HTMLElement>;
    lists.forEach(el => el.scrollTo({ top: 0, behavior: 'smooth' }));
  }

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
    this.publicPosts.update(posts =>
      posts.map(p => (p.postId === postId ? { ...p, ...patch } : p))
    );

    this.privatePosts.update(posts =>
      posts.map(p => (p.postId === postId ? { ...p, ...patch } : p))
    );
  }

  private patchSource(postId: string, patch: Partial<PostSource>): void {
    this.publicPosts.update(posts =>
      posts.map(p => {
        if (p.postId !== postId || !p.sourceIds?.length) return p;
        const updatedSources: PostSource[] = p.sourceIds.map((s, i) =>
          i === 0 ? { ...s, ...patch } : s
        );
        return { ...p, sourceIds: updatedSources };
      })
    );

    this.privatePosts.update(posts =>
      posts.map(p => {
        if (p.postId !== postId || !p.sourceIds?.length) return p;
        const updatedSources: PostSource[] = p.sourceIds.map((s, i) =>
          i === 0 ? { ...s, ...patch } : s
        );
        return { ...p, sourceIds: updatedSources };
      })
    );
  }

  protected readonly Math = Math;
  reportPost(post: Post) {
    this.selectedPostForReport.set(post);
    this.reportDialogVisible.set(true);
  }

  protected readonly ContentTypeEnum = ContentTypeEnum;

  reportSource(postSource: PostSource, post: Post) {
    this.selectedSourceForReport.set(postSource);
    this.reportSourceDialogVisible.set(true);
    this.selectedPostForSourceReport.set(post);
  }
}

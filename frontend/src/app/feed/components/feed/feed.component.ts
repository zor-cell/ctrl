import {Component, inject, OnDestroy, OnInit, signal, ElementRef, ViewChild} from '@angular/core';
import { forkJoin } from 'rxjs';
import { UserService } from '../../../user/user.service';
import { FeedService } from '../../services/feed.service';
import { Post } from '../../dto/Post';
import { PostSource } from '../../dto/PostSource';
import { AlgorithmConfigComponent } from '../../../algorithm/components/config/config.component';

import { TabsModule } from 'primeng/tabs';
import { ReportComponent } from '../../../report/components/report/report.component';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { CommonModule } from '@angular/common';
import { PostService, VoteType } from '../../../post/post.service';
import { ContentTypeEnum } from '../../../report/dto/report';
import { ViewTrackingService } from '../../../post/services/view-tracking.service';
import { PostViewDirective, ViewEvent } from '../../../post/post-view-directive/post-view.directive';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RelationshipService } from '../../../user/relationship.service';
import { FollowBadgeComponent } from '../follow-badge/follow-badge.component';
import { WhyDialogComponent } from '../why-dialog/why-dialog.component';
import { PostDetailDialogComponent } from '../../../post/components/post-detail-dialog/post-detail-dialog.component';
import { PostShareDialogComponent } from '../../../post/components/post-detail-share/post-share-dialog.component';
import { PostDetailSourceComponent } from '../../../post/components/post-detail-source/post-detail-source.component';
import { PostStateService } from '../../../post/services/post-state.service';
import { SessionStateService } from '../../../session/session-state.service';
import { SourceResponse } from '../../../post/dto/PostResponse';

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
    SkeletonModule,
    AlgorithmConfigComponent,
    PostViewDirective,
    ReactiveFormsModule,
    RouterLink,
    ReportComponent,
    FollowBadgeComponent,
    WhyDialogComponent,
    PostDetailDialogComponent,
    PostShareDialogComponent,
    PostDetailSourceComponent,
  ],
  templateUrl: './feed.component.html',
  styleUrl: './feed.component.scss',
})
export class FeedComponent implements OnInit, OnDestroy {
  protected userService = inject(UserService);
  private readonly feedService = inject(FeedService);
  postService = inject(PostService);
  protected sessionState = inject(SessionStateService);

  readonly viewTracking = inject(ViewTrackingService);

  // ─── Shared state service ─────────────────────────────────────────────────
  protected readonly postState = inject(PostStateService);

  protected whyDialogVisible = signal(false);
  protected selectedPost = signal<Post | null>(null);

  protected selectedPostId = signal<string | null>(null);

  // Report dialog
  protected reportDialogVisible = signal(false);
  protected selectedPostForReport = signal<Post | null>(null);

  // Report source
  protected reportSourceDialogVisible = signal(false);
  protected selectedSourceForReport = signal<PostSource | null>(null);
  protected selectedPostForSourceReport = signal<Post | null>(null);
  protected shareDialogVisible = signal(false);
  protected selectedPostForShare = signal<Post | null>(null);

  @ViewChild(PostShareDialogComponent) private shareDialog!: PostShareDialogComponent;

  protected algorithmConfigVisible = signal(false);
  protected isRefreshing = signal(false);
  protected isSpinning = signal(false);
  protected isLoading = signal(true);

  // ─── Infinite Scroll ──────────────────────────────────────────────────────
  private readonly postsPerPage = 20;
  private publicPage  = 0;
  private privatePage = 0;

  protected hasMorePublic  = signal(true);
  protected hasMorePrivate = signal(true);
  protected isLoadingMore  = signal(false);

  private observer!: IntersectionObserver;

  @ViewChild('publicSentinel',  { static: false }) publicSentinelRef!:  ElementRef;
  @ViewChild('privateSentinel', { static: false }) privateSentinelRef!: ElementRef;

  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly rel = inject(RelationshipService);

  protected activeTab = signal<string>(
    localStorage.getItem('feed-active-tab') ?? 'private'
  );

  protected onTabChange(tab: string | number | undefined): void {
    if (tab == null) return;
    const value = String(tab);
    this.activeTab.set(value);
    localStorage.setItem('feed-active-tab', value);
  }

  protected skeletonItems = [1, 2, 3];

  private readonly elRef = inject(ElementRef);

  private readonly votingInProgress = new Set<string>();

  protected publicPosts  = signal<Post[]>([]);
  protected privatePosts = signal<Post[]>([]);
  private readonly relativeTimeNow = signal(Date.now());
  private relativeTimeTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.relativeTimeTimer = setInterval(() => this.relativeTimeNow.set(Date.now()), 30_000);
    this.isLoading.set(true);
    this.userService.loadUser().subscribe();
    this.viewTracking.startTracking();

    forkJoin([
      this.feedService.getFeed(true,  0, this.postsPerPage),
      this.feedService.getFeed(false, 0, this.postsPerPage),
      this.rel.loadFriends(),
      this.rel.loadFollowed(),
      this.rel.loadSentFriendRequests(),
      this.rel.loadIncomingFriendRequests(),
    ]).subscribe({
      next: ([publicPosts, privatePosts]) => {
        this.publicPosts.set(publicPosts);
        this.privatePosts.set(privatePosts);
        this.hasMorePublic.set(publicPosts.length === this.postsPerPage);
        this.hasMorePrivate.set(privatePosts.length === this.postsPerPage);

        this.seedPostState([...publicPosts, ...privatePosts]);

        this.isLoading.set(false);

        setTimeout(() => this.setupObserver(), 0);

        const postId = this.route.snapshot.queryParamMap.get('comments');
        if (postId) {
          this.openCommentsDialog(postId);
        }
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();

    if (this.relativeTimeTimer) {
      clearInterval(this.relativeTimeTimer);
      this.relativeTimeTimer = null;
    }

    this.viewTracking.stopAndClear();
  }

  // ─── Infinite Scroll Setup ────────────────────────────────────────────────

  private setupObserver(): void {
    this.observer?.disconnect();

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting || this.isLoadingMore()) return;

          if (entry.target === this.publicSentinelRef?.nativeElement) {
            if (this.activeTab() === 'public' && this.hasMorePublic()) {
              this.loadMorePublic();
            }
          } else {
            if (this.activeTab() === 'private' && this.hasMorePrivate()) {
              this.loadMorePrivate();
            }
          }
        }
      },
      { threshold: 0.1 }
    );

    if (this.publicSentinelRef?.nativeElement) {
      this.observer.observe(this.publicSentinelRef.nativeElement);
    }
    if (this.privateSentinelRef?.nativeElement) {
      this.observer.observe(this.privateSentinelRef.nativeElement);
    }
  }

  private loadMorePublic(): void {
    this.isLoadingMore.set(true);
    this.publicPage++;

    this.feedService.getFeed(true, this.publicPage, this.postsPerPage).subscribe({
      next: (posts) => {
        this.publicPosts.update(existing => [...existing, ...posts]);
        this.hasMorePublic.set(posts.length === this.postsPerPage);
        this.seedPostState(posts);
        this.isLoadingMore.set(false);
      },
      error: () => {
        this.publicPage--;
        this.isLoadingMore.set(false);
      },
    });
  }

  private loadMorePrivate(): void {
    this.isLoadingMore.set(true);
    this.privatePage++;

    this.feedService.getFeed(false, this.privatePage, this.postsPerPage).subscribe({
      next: (posts) => {
        this.privatePosts.update(existing => [...existing, ...posts]);
        this.hasMorePrivate.set(posts.length === this.postsPerPage);
        this.seedPostState(posts);
        this.isLoadingMore.set(false);
      },
      error: () => {
        this.privatePage--;
        this.isLoadingMore.set(false);
      },
    });
  }

  // ─── Seed post state helper ───────────────────────────────────────────────

  private seedPostState(posts: Post[]): void {
    posts.forEach(post => {
      this.postState.initVote(
        post.postId,
        (post.userVote as 'up' | 'down' | null) ?? null,
        (post.sourceIds?.[0]?.userVote as 'up' | 'down' | null) ?? null,
      );
      this.postState.initCommentCount(post.postId, post.comments?.length ?? 0);
    });
  }

  openAlgorithmConfig(): void {
    this.algorithmConfigVisible.set(true);
  }

  // ─── Comment count helper ─────────────────────────────────────────────────

  protected commentCount(post: Post): number {
    return this.postState.getCommentCount(post.postId) ?? post.comments?.length ?? 0;
  }

  // ─── Vote helpers ─────────────────────────────────────────────────────────

  protected userVoteFor(postId: string): 'up' | 'down' | null {
    return this.postState.getVote(postId).userVote;
  }

  protected sourceVoteFor(postId: string): 'up' | 'down' | null {
    return this.postState.getVote(postId).sourceVote;
  }

  async votePost(post: Post, voteType: VoteType): Promise<void> {
    if (this.votingInProgress.has(post.postId)) return;
    this.votingInProgress.add(post.postId);

    const previousVote      = this.postState.getVote(post.postId).userVote;
    const previousUpvotes   = post.upvotes;
    const previousDownvotes = post.downvotes;

    const isSameVote = previousVote === voteType;
    const optimistic = this.calcOptimisticVote(post.upvotes, post.downvotes, previousVote, voteType, isSameVote);

    this.postState.setUserVote(post.postId, optimistic.userVote);
    this.patchPost(post.postId, { upvotes: optimistic.upvotes, downvotes: optimistic.downvotes });

    try {
      const result = await this.postService.votePost(post.postId, voteType);
      this.postState.setUserVote(post.postId, result.userVote ?? null);
      this.patchPost(post.postId, {
        upvotes: result.upvotes,
        downvotes: result.downvotes,
      });
    } catch (err) {
      console.error('Post-vote failed; will be reset:', err);
      this.postState.setUserVote(post.postId, previousVote);
      this.patchPost(post.postId, {
        upvotes: previousUpvotes,
        downvotes: previousDownvotes,
      });
    } finally {
      this.votingInProgress.delete(post.postId);
    }
  }

  // ─── Source adapter ───────────────────────────────────────────────────────

  /**
   * Maps the feed's PostSource[] to the SourceResponse shape expected by
   * PostDetailSourceComponent. Returns null when the post has no sources.
   */
  protected toSourceResponse(post: Post): SourceResponse | null {
    const sources = post.sourceIds;
    if (!sources?.length) return null;

    const first = sources[0];
    return {
      sourceId:          first.sourceId,
      upvotes:           first.upvotes   ?? 0,
      downvotes:         first.downvotes ?? 0,
      createdAt:         first.createdAt ?? '',
      wasReportedByUser: first.wasReportedByUser ?? false,
      singleSources:     sources.map(s => ({
        singleSourceId: s.sourceId,
        journal:        s.journal        ?? null,
        author:         s.author         ?? null,
        title:          s.title          ?? null,
        url:            s.url            ?? null,
        date:           s.date           ?? null,
      })),
    };
  }

  getDomain(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
  }

  protected timeAgo(createdAt: string): string {
    const normalized = createdAt.endsWith('Z') ? createdAt : createdAt + 'Z';
    const date = new Date(normalized);
    const diffMs = this.relativeTimeNow() - date.getTime();
    const minutes = Math.floor(diffMs / 60_000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;
    const weeks = Math.floor(days / 7);
    if (weeks < 4) return `${weeks}w ago`;
    const months = Math.floor(days / 30);
    if (months < 6) return `${months}mo ago`;
    return date.toLocaleDateString('de-AT', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  openWhyDialog(post: Post): void {
    this.selectedPost.set(post);
    this.whyDialogVisible.set(true);
  }

  openCommentsDialog(postId: string): void {
    this.selectedPostId.set(postId);
    this.router.navigate([], {
      queryParams: { comments: postId },
      queryParamsHandling: 'merge',
    });
  }

  onCommentsDialogDismissed(): void {
    this.selectedPostId.set(null);
    this.router.navigate([], {
      queryParams: { comments: null },
      queryParamsHandling: 'merge',
    });
  }

  onPostDeleted(postId: string): void {
    this.removePostFromFeed(postId);
    this.onCommentsDialogDismissed();
  }

  isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)(?:[?#].*)?$/i.test(url);
  }

  onPostViewed(event: ViewEvent): void {
    this.viewTracking.track(event);
  }

  protected loadFeed(): void {
    this.publicPage  = 0;
    this.privatePage = 0;

    this.isLoading.set(true);
    this.isSpinning.set(true);
    setTimeout(() => this.isSpinning.set(false), 600);

    forkJoin([
      this.feedService.getFeed(true,  0, this.postsPerPage),
      this.feedService.getFeed(false, 0, this.postsPerPage),
    ]).subscribe({
      next: ([publicPosts, privatePosts]) => {
        this.publicPosts.set(publicPosts);
        this.privatePosts.set(privatePosts);
        this.hasMorePublic.set(publicPosts.length === this.postsPerPage);
        this.hasMorePrivate.set(privatePosts.length === this.postsPerPage);

        [...publicPosts, ...privatePosts].forEach(post => {
          this.postState.setUserVote(post.postId, (post.userVote as 'up' | 'down' | null) ?? null);
          this.postState.setSourceVote(post.postId, (post.sourceIds?.[0]?.userVote as 'up' | 'down' | null) ?? null);
          this.postState.setCommentCount(post.postId, post.comments?.length ?? 0);
        });

        this.sessionState.resetLimit();

        this.isLoading.set(false);
        this.isRefreshing.set(false);
        this.scrollFeedToTop();

        setTimeout(() => this.setupObserver(), 0);
      },
      error: () => {
        this.isLoading.set(false);
        this.isRefreshing.set(false);
        this.publicPosts.set([]);
        this.privatePosts.set([]);
      },
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

  reportPost(post: Post): void {
    this.selectedPostForReport.set(post);
    this.reportDialogVisible.set(true);
  }

  protected onReportSubmitted(event: { contentType: ContentTypeEnum; contentId: string }): void {
    if (event.contentType !== ContentTypeEnum.POST) {
      return;
    }
    this.removePostFromFeed(event.contentId);
    this.selectedPostForReport.set(null);
  }

  private removePostFromFeed(postId: string): void {
    this.publicPosts.update(posts => posts.filter(post => post.postId !== postId));
    this.privatePosts.update(posts => posts.filter(post => post.postId !== postId));
  }

  protected readonly ContentTypeEnum = ContentTypeEnum;

  reportSource(postSource: PostSource, post: Post): void {
    this.selectedSourceForReport.set(postSource);
    this.reportSourceDialogVisible.set(true);
    this.selectedPostForSourceReport.set(post);
  }

  protected openShareDialog(post: Post): void {
    this.selectedPostForShare.set(post);
    this.shareDialogVisible.set(true);
    this.shareDialog?.open();
  }

  protected onShareDialogVisibleChange(visible: boolean): void {
    this.shareDialogVisible.set(visible);
    if (!visible) {
      this.selectedPostForShare.set(null);
    }
  }
}

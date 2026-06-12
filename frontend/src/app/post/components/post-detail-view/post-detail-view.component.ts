import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  signal,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { DetailedPostResponse } from '../../dto/DetailedPostResponse';
import { PostService, VoteType } from '../../post.service';
import { UserService } from '../../../user/user.service';
import { ReportComponent } from '../../../report/components/report/report.component';
import { ContentTypeEnum } from '../../../report/dto/report';
import { FollowBadgeComponent } from '../../../feed/components/follow-badge/follow-badge.component';
import { PostShareDialogComponent } from '../post-detail-share/post-share-dialog.component';
import { PostCommentComponent} from '../post-detail-comment/post-comment.component';
import { PostStateService } from '../../services/post-state.service';
import { MediaCacheService } from '../../../core/services/media-cache.service';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-post-detail-view',
  standalone: true,
  imports: [
    CommonModule,
    AvatarModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    ProgressSpinnerModule,
    ReportComponent,
    FollowBadgeComponent,
    PostShareDialogComponent,
    PostCommentComponent,
  ],
  templateUrl: './post-detail-view.component.html',
  styleUrl: './post-detail-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostDetailViewComponent implements OnChanges {
  private readonly router = inject(Router);
  private readonly relativeTimeNow = signal(Date.now());

  private readonly postService  = inject(PostService);
  private readonly userService  = inject(UserService);
  private readonly cdr          = inject(ChangeDetectorRef);
  private readonly postState    = inject(PostStateService);
  private readonly mediaCache   = inject(MediaCacheService);

  @Input() postId: string | null = null;
  @Output() readonly shareClicked  = new EventEmitter<void>();
  @Output() readonly closeClicked  = new EventEmitter<void>();

  @ViewChild(PostShareDialogComponent) private readonly shareDialog!: PostShareDialogComponent;
  @ViewChild('commentsComponent') protected commentsComponent?: PostCommentComponent;

  protected readonly post    = signal<DetailedPostResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error   = signal(false);

  // ─── Voting: computed signals from shared store (OnPush-compatible) ──────────

  private readonly _postId = signal<string | null>(null);

  protected readonly postVote   = computed(() =>
    this.postState.voteMap()[this._postId() ?? '']?.userVote  ?? null
  );
  protected readonly sourceVote = computed(() =>
    this.postState.voteMap()[this._postId() ?? '']?.sourceVote ?? null
  );

  protected readonly postUpvotes   = signal(0);
  protected readonly postDownvotes = signal(0);

  private votingInProgress       = false;
  private sourceVotingInProgress = false;

  // ─── Report ───────────────────────────────────────────────────────────────
  protected readonly reportPostVisible   = signal(false);
  protected readonly reportSourceVisible = signal(false);
  protected readonly ContentTypeEnum     = ContentTypeEnum;

  // ─── Share ────────────────────────────────────────────────────────────────
  protected readonly shareDialogVisible = signal(false);

  // ─── Lifecycle ────────────────────────────────────────────────────────────

  ngOnChanges(changes: SimpleChanges): void {
    const change   = changes['postId'];
    const current  = change?.currentValue as string | null;
    const previous = change?.previousValue as string | null;
    this._postId.set(current);
    if (current && current !== previous) this.fetchPost(current);
    if (!current) this.reset();
  }

  // ─── Data fetching ────────────────────────────────────────────────────────

  private async fetchPost(postId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    this.post.set(null);
    try {
      const post = await this.postService.getPostById(postId);

      // Resolve signed/cached URLs from the garage container
      if (post.mediaUrl)           post.mediaUrl           = await this.resolveMedia(post.mediaUrl)           ?? post.mediaUrl;
      if (post.userProfilePicture) post.userProfilePicture = await this.resolveMedia(post.userProfilePicture) ?? post.userProfilePicture;

      this.post.set(post);

      const sourceVoteFromDetail = (post.source as { userVote?: 'up' | 'down' | null })?.userVote ?? null;
      this.postState.initVote(postId, post.userVote ?? null, sourceVoteFromDetail);

      const commentCount = post.comments?.length ?? 0;
      this.postState.setCommentCount(postId, commentCount);

      this.postUpvotes.set(post.upvotes ?? 0);
      this.postDownvotes.set(post.downvotes ?? 0);
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
      this.cdr.markForCheck();
    }
  }

  private resolveMedia(url: string | null | undefined): Promise<string | null> {
    if (!url) return Promise.resolve(null);
    const kind = /\.(mp4|webm|mov)(\?|#|$)/i.test(url) ? 'video' : 'image';
    return firstValueFrom(this.mediaCache.resolveMediaUrl(url, kind, 'high')).catch(() => null);
  }

  private reset(): void {
    this.post.set(null);
    this.loading.set(false);
    this.error.set(false);
    this.postUpvotes.set(0);
    this.postDownvotes.set(0);
  }

  // ─── Comment count (from shared store) ────────────────────────────────────

  /**
   * Called by the template. Returns the live comment count from the shared
   * store so that both the detail-view action bar and the feed card stay
   * in sync without a round-trip.
   */
  protected get liveCommentCount(): number {
    const fromStore = this.postState.getCommentCount(this.postId ?? '');
    return fromStore ?? this.commentsComponent?.count ?? 0;
  }

  /**
   * Called by PostCommentComponent (or its parent) whenever the user
   * successfully submits a comment.
   */
  protected onCommentAdded(): void {
    if (this.postId) {
      this.postState.incrementCommentCount(this.postId);
    }
    this.cdr.markForCheck();
  }

  // ─── Voting ───────────────────────────────────────────────────────────────

  protected async votePost(voteType: VoteType): Promise<void> {
    const post = this.post();
    if (!post || this.votingInProgress) return;
    this.votingInProgress = true;

    const prevVote = this.postVote();
    const prevUp   = this.postUpvotes();
    const prevDown = this.postDownvotes();

    const isSameVote = prevVote === voteType;
    const optimistic = this.calcOptimisticVote(prevUp, prevDown, prevVote, voteType, isSameVote);

    // Write to shared store (feed will reflect this immediately)
    this.postState.setUserVote(post.postId, optimistic.userVote);
    this.postUpvotes.set(optimistic.upvotes);
    this.postDownvotes.set(optimistic.downvotes);
    this.cdr.markForCheck();

    try {
      const result = await this.postService.votePost(post.postId, voteType);
      this.postState.setUserVote(post.postId, result.userVote ?? null);
      this.postUpvotes.set(result.upvotes ?? optimistic.upvotes);
      this.postDownvotes.set(result.downvotes ?? optimistic.downvotes);
    } catch (err) {
      console.error('Post-vote failed; reverting:', err);
      this.postState.setUserVote(post.postId, prevVote);
      this.postUpvotes.set(prevUp);
      this.postDownvotes.set(prevDown);
    } finally {
      this.votingInProgress = false;
      this.cdr.markForCheck();
    }
  }

  protected async voteSource(voteType: VoteType): Promise<void> {
    const sourceId = this.post()?.source?.sourceId;
    if (!sourceId || this.sourceVotingInProgress) return;
    this.sourceVotingInProgress = true;

    const prevVote   = this.sourceVote();
    const isSameVote = prevVote === voteType;

    // Write to shared store (feed will reflect this immediately)
    this.postState.setSourceVote(this.postId!, isSameVote ? null : voteType);
    this.cdr.markForCheck();

    try {
      const result = await this.postService.voteSource(sourceId, voteType);
      this.postState.setSourceVote(this.postId!, result.userVote ?? null);
    } catch (err) {
      console.error('Source-vote failed; reverting:', err);
      this.postState.setSourceVote(this.postId!, prevVote);
    } finally {
      this.sourceVotingInProgress = false;
      this.cdr.markForCheck();
    }
  }

  private calcOptimisticVote(
    upvotes: number,
    downvotes: number,
    currentVote: 'up' | 'down' | null,
    voteType: VoteType,
    isSameVote: boolean,
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

  // ─── Share ────────────────────────────────────────────────────────────────

  protected openShareDialog(): void {
    this.shareDialogVisible.set(true);
    this.shareDialog?.open();
    this.shareClicked.emit();
  }

  protected onShareDialogVisibleChange(visible: boolean): void {
    this.shareDialogVisible.set(visible);
  }

  // ─── Post ownership ───────────────────────────────────────────────────────

  protected isOwnPost(): boolean {
    return this.post()?.username === this.userService.user()?.username;
  }

  // ─── Utilities ────────────────────────────────────────────────────────────

  protected isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)(?:[?#].*)?$/i.test(url);
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

  protected getDomain(url: string | null): string {
    if (!url) return '';
    try   { return new URL(url).hostname.replace(/^www\./, ''); }
    catch { return url; }
  }

  protected goToUser(): void {
    const p = this.post();
    if (!p) return;
    if (p.isExternal) {
      window.open(p.userProfileUrl!);
    } else {
      this.router.navigate(['/profile', p.username]);
    }
  }

}

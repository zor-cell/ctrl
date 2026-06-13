import {Component, computed, effect, inject, OnDestroy, OnInit, signal, ElementRef} from '@angular/core';
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
import { SkeletonModule } from 'primeng/skeleton';
import { CommonModule } from '@angular/common';
import { PostService, VoteType } from '../../../post/post.service';
import {ContentTypeEnum} from '../../../report/dto/report';
import {ViewTrackingService} from '../../../post/services/view-tracking.service';
import {PostViewDirective, ViewEvent} from '../../../post/post-view-directive/post-view.directive';
import {ReactiveFormsModule} from "@angular/forms";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {RelationshipService} from '../../../user/relationship.service';
import {Friend} from '../../../user/dto/Relationship';
import {FollowBadgeComponent} from '../follow-badge/follow-badge.component';
import {WhyDialogComponent} from '../why-dialog/why-dialog.component';
import {MediaCacheService} from '../../../core/services/media-cache.service';
import {usernameInitial} from '../../../core/utils/username-initial.utils';
import { Chat, ChatParticipant } from '../../../chat/models/chat.models';
import { ChatService } from '../../../chat/services/chat.service';

interface SharedPostPayload {
  postId: string;
  username: string;
  caption: string;
  url: string;
  mediaUrl: string | null;
  mediaKind: 'image' | 'video' | null;
}

interface ShareTarget {
  id: string;
  kind: 'direct' | 'group';
  chatId: string | null;
  username: string | null;
  title: string;
  subtitle: string;
  avatarRef: string | null;
  interactionCount: number;
  lastActivityTime: number;
}
import {SessionStateService} from "../../../session/session-state.service";

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
    CommentsComponent,
    AlgorithmConfigComponent,
    PostViewDirective,
    ReactiveFormsModule,
    RouterLink,
    ReportComponent,
    FollowBadgeComponent,
    WhyDialogComponent,
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
  protected shareDialogVisible = signal(false);
  protected selectedPostForShare = signal<Post | null>(null);
  protected shareFriendQuery = signal('');

  protected algorithmConfigVisible = signal(false);
  protected isRefreshing = signal(false);
  protected isSpinning = signal(false);
  protected isLoading = signal(true);

  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly rel = inject(RelationshipService);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly chatService = inject(ChatService);
  protected readonly friends = this.rel.friends;
  protected readonly shareChats = signal<Chat[]>([]);
  protected readonly shareFriendAvatarUrls = signal<Record<string, string>>({});
  protected readonly shareGroupAvatarUrls = signal<Record<string, string>>({});
  protected readonly failedShareFriendAvatars = signal<Set<string>>(new Set());
  protected readonly failedShareGroupAvatars = signal<Set<string>>(new Set());
  protected readonly filteredShareTargets = computed(() => {
    const query = this.shareFriendQuery().trim().toLowerCase();
    return this.shareTargets().filter(target =>
      !query
      || target.title.toLowerCase().includes(query)
      || target.subtitle.toLowerCase().includes(query)
    );
  });
  constructor() {
    effect(() => {
      this.loadShareFriendAvatars(this.friends());
    });
    effect(() => {
      this.loadShareGroupAvatars(this.shareChats());
    });
  }

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

  // Prevents double-clicks whilst a request is in progress
  private readonly votingInProgress = new Set<string>();
  private readonly sourceVotingInProgress = new Set<string>();

  protected publicPosts = signal<Post[]>([]);
  protected privatePosts = signal<Post[]>([]);
  private readonly relativeTimeNow = signal(Date.now());
  private relativeTimeTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.relativeTimeTimer = setInterval(() => this.relativeTimeNow.set(Date.now()), 30_000);
    this.isLoading.set(true);
    this.userService.loadUser().subscribe();
    this.viewTracking.startTracking();
    this.loadShareChats();

    forkJoin([
      this.feedService.getFeed(true),
      this.feedService.getFeed(false),
      this.rel.loadFriends(),
      this.rel.loadFollowed(),
      this.rel.loadSentFriendRequests(),
      this.rel.loadIncomingFriendRequests(),
    ]).subscribe({
      next: ([publicPosts, privatePosts]) => {
        this.publicPosts.set(publicPosts);
        this.privatePosts.set(privatePosts);
        this.isLoading.set(false);

        const postId = this.route.snapshot.queryParamMap.get('comments');
        if (postId) {
          const post = [...publicPosts, ...privatePosts]
            .find(p => p.postId === postId);
          if (post) this.openCommentsDialog(post);
        }
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    if (this.relativeTimeTimer) {
      clearInterval(this.relativeTimeTimer);
      this.relativeTimeTimer = null;
    }

    this.viewTracking.stopAndClear();
  }

  openAlgorithmConfig(): void {
    this.algorithmConfigVisible.set(true);
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
    const diffMs = this.relativeTimeNow() - new Date(normalized).getTime();
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

  openCommentsDialog(post: Post): void {
    this.selectedPostForComments.set(post);
    this.commentsDialogVisible.set(true);
    this.router.navigate([], {
      queryParams: { comments: post.postId },
      queryParamsHandling: 'merge',
    });
  }

  onCommentsVisibleChange(visible: boolean): void {
    this.commentsDialogVisible.set(visible);
    if (!visible) {
      this.router.navigate([], {
        queryParams: { comments: null },
        queryParamsHandling: 'merge',
      });
    }
  }

  isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)(?:[?#].*)?$/i.test(url);
  }

  onPostViewed(event: ViewEvent): void {
    this.viewTracking.track(event);
  }

  /** Helpers **/
  protected loadFeed() {
    this.isLoading.set(true);
    this.isSpinning.set(true);
    setTimeout(() => this.isSpinning.set(false), 600);

    forkJoin([
      this.feedService.getFeed(true),
      this.feedService.getFeed(false),
    ]).subscribe({
      next: ([publicPosts, privatePosts]) => {
        this.publicPosts.set(publicPosts);
        this.privatePosts.set(privatePosts);

        this.sessionState.resetLimit();

        this.isLoading.set(false);
        this.isRefreshing.set(false);
        this.scrollFeedToTop();
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

  reportPost(post: Post) {
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

  reportSource(postSource: PostSource, post: Post) {
    this.selectedSourceForReport.set(postSource);
    this.reportSourceDialogVisible.set(true);
    this.selectedPostForSourceReport.set(post);
  }

  protected openShareDialog(post: Post): void {
    this.selectedPostForShare.set(post);
    this.shareFriendQuery.set('');
    this.loadShareChats();
    this.shareDialogVisible.set(true);
  }

  protected sharePostWithTarget(target: ShareTarget): void {
    const post = this.selectedPostForShare();
    if (!post) {
      return;
    }

    this.shareDialogVisible.set(false);
    const queryParams = target.chatId
      ? { chat: target.chatId }
      : { user: target.username };

    this.router.navigate(['/chat'], {
      queryParams,
      state: { sharedPost: this.sharedPostPayload(post) },
    });
  }

  protected onShareDialogVisibleChange(visible: boolean): void {
    this.shareDialogVisible.set(visible);
    if (!visible) {
      this.selectedPostForShare.set(null);
      this.shareFriendQuery.set('');
    }
  }

  protected shareFriendAvatarUrl(friend: Friend): string | null {
    return this.shareFriendAvatarUrls()[friend.id] ?? null;
  }

  protected shareFriendInitial(friend: Friend): string {
    return usernameInitial(friend.username);
  }

  protected shareFriendAvatarFailed(friend: Friend): boolean {
    return this.failedShareFriendAvatars().has(friend.id);
  }

  protected markShareFriendAvatarFailed(friend: Friend): void {
    this.failedShareFriendAvatars.update(failed => {
      const next = new Set(failed);
      next.add(friend.id);
      return next;
    });
  }

  protected shareTargetAvatarUrl(target: ShareTarget): string | null {
    if (target.kind === 'group' && target.chatId) {
      return this.shareGroupAvatarUrls()[target.chatId] ?? null;
    }

    if (!target.username) {
      return null;
    }

    const friend = this.friendByUsername(target.username);
    return friend ? this.shareFriendAvatarUrl(friend) : null;
  }

  protected shareTargetAvatarFailed(target: ShareTarget): boolean {
    if (target.kind === 'group' && target.chatId) {
      return this.failedShareGroupAvatars().has(target.chatId);
    }

    if (!target.username) {
      return false;
    }

    const friend = this.friendByUsername(target.username);
    return friend ? this.shareFriendAvatarFailed(friend) : false;
  }

  protected markShareTargetAvatarFailed(target: ShareTarget): void {
    if (target.kind === 'group' && target.chatId) {
      this.failedShareGroupAvatars.update(failed => {
        const next = new Set(failed);
        next.add(target.chatId!);
        return next;
      });
      return;
    }

    if (!target.username) {
      return;
    }

    const friend = this.friendByUsername(target.username);
    if (friend) {
      this.markShareFriendAvatarFailed(friend);
    }
  }

  protected shareTargetInitial(target: ShareTarget): string {
    return usernameInitial(target.username ?? target.title);
  }

  protected shareTargetIcon(target: ShareTarget): string {
    return target.kind === 'group' ? 'pi-users' : 'pi-user';
  }

  private loadShareFriendAvatars(friends: Friend[]): void {
    friends.forEach(friend => {
      if (!friend.profilePicture || this.shareFriendAvatarUrls()[friend.id]) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(friend.profilePicture, 'image', 'high')
        .subscribe(url => {
          if (!url) {
            return;
          }
          this.shareFriendAvatarUrls.update(urls => ({ ...urls, [friend.id]: url }));
        });
    });
  }

  private loadShareGroupAvatars(chats: Chat[]): void {
    chats
      .filter(chat => chat.groupChat)
      .forEach(chat => {
        if (!chat.groupImage || this.shareGroupAvatarUrls()[chat.chatId]) {
          return;
        }

        this.mediaCacheService.resolveMediaUrl(chat.groupImage, 'image', 'high')
          .subscribe(url => {
            if (!url) {
              return;
            }
            this.shareGroupAvatarUrls.update(urls => ({ ...urls, [chat.chatId]: url }));
          });
      });
  }

  private loadShareChats(): void {
    this.chatService.getChats().subscribe({
      next: chats => this.shareChats.set(chats.map(chat => ({
        ...chat,
        personalMessageCount: chat.personalMessageCount ?? 0,
      }))),
    });
  }

  private shareTargets(): ShareTarget[] {
    const friendsByUsername = new Map(
      this.friends().map(friend => [friend.username.toLowerCase(), friend])
    );
    const targets = this.shareChats()
      .map(chat => this.shareTargetFromChat(chat, friendsByUsername))
      .filter((target): target is ShareTarget => target !== null);
    const usernamesWithChats = new Set(
      targets
        .filter(target => target.kind === 'direct' && target.username)
        .map(target => target.username!.toLowerCase())
    );
    const friendsWithoutChats = this.friends()
      .filter(friend => !usernamesWithChats.has(friend.username.toLowerCase()))
      .map(friend => this.shareTargetFromFriend(friend));

    return [...targets, ...friendsWithoutChats].sort((left, right) => {
      if (left.interactionCount !== right.interactionCount) {
        return right.interactionCount - left.interactionCount;
      }
      if (left.lastActivityTime !== right.lastActivityTime) {
        return right.lastActivityTime - left.lastActivityTime;
      }
      return left.title.localeCompare(right.title);
    });
  }

  private shareTargetFromChat(
    chat: Chat,
    friendsByUsername: Map<string, Friend>,
  ): ShareTarget | null {
    if (chat.groupChat) {
      return {
        id: `chat:${chat.chatId}`,
        kind: 'group',
        chatId: chat.chatId,
        username: null,
        title: chat.name,
        subtitle: `${chat.participants.length} ${chat.participants.length === 1 ? 'member' : 'members'}`,
        avatarRef: chat.groupImage,
        interactionCount: chat.personalMessageCount ?? 0,
        lastActivityTime: this.shareChatActivityTime(chat),
      };
    }

    const other = this.shareDirectPartner(chat);
    if (!other) {
      return null;
    }

    const friend = friendsByUsername.get(other.username.toLowerCase());
    return {
      id: `chat:${chat.chatId}`,
      kind: 'direct',
      chatId: chat.chatId,
      username: other.username,
      title: other.name?.trim() || friend?.name || other.username,
      subtitle: `@${other.username}`,
      avatarRef: other.profilePicture ?? friend?.profilePicture ?? null,
      interactionCount: chat.personalMessageCount ?? 0,
      lastActivityTime: this.shareChatActivityTime(chat),
    };
  }

  private shareTargetFromFriend(friend: Friend): ShareTarget {
    return {
      id: `friend:${friend.username}`,
      kind: 'direct',
      chatId: null,
      username: friend.username,
      title: friend.name || friend.username,
      subtitle: `@${friend.username}`,
      avatarRef: friend.profilePicture,
      interactionCount: 0,
      lastActivityTime: 0,
    };
  }

  private shareDirectPartner(chat: Chat): ChatParticipant | null {
    const currentUserId = this.userService.user()?.id?.toLowerCase();
    const byChatName = chat.participants.find(participant => participant.username === chat.name);
    if (byChatName) {
      return byChatName;
    }

    if (currentUserId) {
      return chat.participants.find(participant => participant.id.toLowerCase() !== currentUserId) ?? null;
    }

    return chat.participants[0] ?? null;
  }

  private shareChatActivityTime(chat: Chat): number {
    const value = chat.lastMessage?.date ?? chat.createdAt;
    return new Date(value.endsWith('Z') ? value : `${value}Z`).getTime();
  }

  private friendByUsername(username: string): Friend | null {
    const normalizedUsername = username.toLowerCase();
    return this.friends().find(friend => friend.username.toLowerCase() === normalizedUsername) ?? null;
  }

  private sharedPostPayload(post: Post): SharedPostPayload {
    const caption = post.caption.length > 220
      ? `${post.caption.slice(0, 217)}...`
      : post.caption;
    const postUrl = `${window.location.origin}/feed?comments=${encodeURIComponent(post.postId)}`;
    const mediaUrl = post.videoImageRef?.[0] ?? null;
    return {
      postId: post.postId,
      username: post.username,
      caption,
      url: postUrl,
      mediaUrl,
      mediaKind: mediaUrl ? (this.isVideo(mediaUrl) ? 'video' : 'image') : null,
    };
  }
}

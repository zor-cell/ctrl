import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';

import { DialogModule } from 'primeng/dialog';
import { CommonModule } from '@angular/common';

import { UserService } from '../../../user/user.service';
import { RelationshipService } from '../../../user/relationship.service';
import { MediaCacheService } from '../../../core/services/media-cache.service';
import { ChatService} from '../../../chat/services/chat.service';
import { usernameInitial } from '../../../core/utils/username-initial.utils';
import { ShareablePost, resolveShareMediaUrl } from '../../dto/ShareablePost';
import { Blocked, Friend } from '../../../user/dto/Relationship';
import { Chat, ChatParticipant} from '../../../chat/models/chat.models';
import {ShareTarget} from '../../dto/ShareTarget';
import {SharedPostPayload} from '../../dto/SharedPostPayload';

@Component({
  selector: 'app-post-share-dialog',
  standalone: true,
  imports: [CommonModule, DialogModule],
  templateUrl: './post-share-dialog.component.html',
  styleUrl: './post-share-dialog.component.scss',
})
export class PostShareDialogComponent {
  // ── Inputs ────────────────────────────────────────────────────────────────
  post    = input<ShareablePost | null>(null);
  visible = input<boolean>(false);

  // ── Outputs ───────────────────────────────────────────────────────────────
  visibleChange = output<boolean>();

  // ── Services ──────────────────────────────────────────────────────────────
  private readonly router            = inject(Router);
  private readonly userService       = inject(UserService);
  private readonly rel               = inject(RelationshipService);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly chatService       = inject(ChatService);

  // ── State ─────────────────────────────────────────────────────────────────
  protected readonly friends      = signal<Friend[]>([]);
  protected readonly blockedUsers = signal<Blocked[]>([]);
  protected readonly shareChats   = signal<Chat[]>([]);

  protected readonly shareFriendQuery       = signal('');
  protected readonly shareFriendAvatarUrls  = signal<Record<string, string>>({});
  protected readonly shareGroupAvatarUrls   = signal<Record<string, string>>({});
  protected readonly failedShareFriendAvatars = signal<Set<string>>(new Set());
  protected readonly failedShareGroupAvatars  = signal<Set<string>>(new Set());

  protected readonly filteredShareTargets = computed(() => {
    const query = this.shareFriendQuery().trim().toLowerCase();
    return this.shareTargets().filter(
      target =>
        !query ||
        target.title.toLowerCase().includes(query) ||
        target.subtitle.toLowerCase().includes(query),
    );
  });

  constructor() {
    effect(() => this.loadShareFriendAvatars(this.friends()));
    effect(() => this.loadShareGroupAvatars(this.shareChats()));
  }

  // ── Public API (called by parent via open()) ──────────────────────────────
  open(): void {
    this.shareFriendQuery.set('');
    this.friends.set([]);
    this.blockedUsers.set([]);
    this.shareChats.set([]);
    this.loadShareTargets();
  }

  // ── Dialog visibility ─────────────────────────────────────────────────────
  protected onVisibleChange(visible: boolean): void {
    this.visibleChange.emit(visible);
    if (!visible) {
      this.shareFriendQuery.set('');
    }
  }

  // ── Share action ──────────────────────────────────────────────────────────
  protected sharePostWithTarget(target: ShareTarget): void {
    const post = this.post();
    if (!post) return;

    this.visibleChange.emit(false);
    const queryParams = target.chatId
      ? { chat: target.chatId }
      : { user: target.username };

    this.router.navigate(['/chat'], {
      queryParams,
      state: { sharedPost: this.sharedPostPayload(post) },
    });
  }

  // ── Avatar helpers ────────────────────────────────────────────────────────
  protected shareTargetAvatarUrl(target: ShareTarget): string | null {
    if (target.kind === 'group' && target.chatId) {
      return this.shareGroupAvatarUrls()[target.chatId] ?? null;
    }
    if (!target.username) return null;
    const friend = this.friendByUsername(target.username);
    return friend ? (this.shareFriendAvatarUrls()[friend.id] ?? null) : null;
  }

  protected shareTargetAvatarFailed(target: ShareTarget): boolean {
    if (target.kind === 'group' && target.chatId) {
      return this.failedShareGroupAvatars().has(target.chatId);
    }
    if (!target.username) return false;
    const friend = this.friendByUsername(target.username);
    return friend ? this.failedShareFriendAvatars().has(friend.id) : false;
  }

  protected markShareTargetAvatarFailed(target: ShareTarget): void {
    if (target.kind === 'group' && target.chatId) {
      this.failedShareGroupAvatars.update(s => new Set([...s, target.chatId!]));
      return;
    }
    if (!target.username) return;
    const friend = this.friendByUsername(target.username);
    if (friend) {
      this.failedShareFriendAvatars.update(s => new Set([...s, friend.id]));
    }
  }

  protected shareTargetInitial(target: ShareTarget): string {
    return usernameInitial(target.username ?? target.title);
  }

  protected shareTargetIcon(target: ShareTarget): string {
    return target.kind === 'group' ? 'pi-users' : 'pi-user';
  }

  // ── Private helpers ───────────────────────────────────────────────────────
  private shareTargets(): ShareTarget[] {
    const unavailableUserIds = this.unavailableUserIdsFromChats();
    const activeFriends = this.friends().filter(friend =>
      this.isShareableFriend(friend, unavailableUserIds),
    );
    const friendsByUsername = new Map(
      activeFriends.map(f => [f.username.toLowerCase(), f]),
    );

    const targets = this.uniqueShareTargets(this.shareChats()
      .map(chat => this.shareTargetFromChat(chat, friendsByUsername))
      .filter((t): t is ShareTarget => t !== null));

    const usernamesWithChats = new Set(
      targets
        .filter(t => t.kind === 'direct' && t.username)
        .map(t => t.username!.toLowerCase()),
    );

    const friendsWithoutChats = activeFriends
      .filter(f => !usernamesWithChats.has(f.username.toLowerCase()))
      .map(f => this.shareTargetFromFriend(f));

    return [...targets, ...friendsWithoutChats].sort((a, b) => {
      if (a.interactionCount !== b.interactionCount) return b.interactionCount - a.interactionCount;
      if (a.lastActivityTime !== b.lastActivityTime) return b.lastActivityTime - a.lastActivityTime;
      return a.title.localeCompare(b.title);
    });
  }

  private uniqueShareTargets(targets: ShareTarget[]): ShareTarget[] {
    const seen = new Set<string>();

    return targets.filter(target => {
      if (seen.has(target.id)) return false;
      seen.add(target.id);
      return true;
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
    if (!this.isShareableParticipant(other)) return null;

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
    const byChatName = chat.participants.find(p => p.username === chat.name);
    if (byChatName) return byChatName;
    if (currentUserId) {
      return chat.participants.find(p => p.id.toLowerCase() !== currentUserId) ?? null;
    }
    return chat.participants[0] ?? null;
  }

  private isShareableFriend(friend: Friend, unavailableUserIds: Set<string>): boolean {
    return !this.isUnavailableUsername(friend.username)
      && !unavailableUserIds.has(friend.id.toLowerCase())
      && !this.isBlockedUser(friend.id, friend.username);
  }

  private isShareableParticipant(participant: ChatParticipant | null): participant is ChatParticipant {
    return !!participant
      && !participant.deleted
      && !this.isUnavailableUsername(participant.username)
      && !this.isBlockedUser(participant.id, participant.username);
  }

  private unavailableUserIdsFromChats(): Set<string> {
    const ids = new Set<string>();
    this.shareChats().forEach(chat =>
      chat.participants.forEach(participant => {
        if (participant.deleted || this.isUnavailableUsername(participant.username)) {
          ids.add(participant.id.toLowerCase());
        }
      }),
    );
    return ids;
  }

  private isUnavailableUsername(username: string | null | undefined): boolean {
    const normalized = username?.trim().toLowerCase();
    return !normalized || normalized === 'deleted user';
  }

  private isBlockedUser(userId: string, username: string): boolean {
    const normalizedId = userId.toLowerCase();
    const normalizedUsername = username.toLowerCase();
    return this.blockedUsers().some(blocked =>
      blocked.id.toLowerCase() === normalizedId
      || blocked.username.toLowerCase() === normalizedUsername,
    );
  }

  private shareChatActivityTime(chat: Chat): number {
    const value = chat.lastMessage?.date ?? chat.createdAt;
    return new Date(value.endsWith('Z') ? value : `${value}Z`).getTime();
  }

  private friendByUsername(username: string): Friend | null {
    const normalized = username.toLowerCase();
    return this.friends().find(f => f.username.toLowerCase() === normalized) ?? null;
  }

  private sharedPostPayload(post: ShareablePost): SharedPostPayload {
    const caption =
      post.caption.length > 220 ? `${post.caption.slice(0, 217)}...` : post.caption;
    const postUrl  = `${window.location.origin}/feed?comments=${encodeURIComponent(post.postId)}`;
    const mediaUrl = resolveShareMediaUrl(post);
    const isVideo  = (url: string) => /\.(mp4|webm|ogg|mov)(?:[?#].*)?$/i.test(url);
    return {
      postId:    post.postId,
      username:  post.username,
      caption,
      url:       postUrl,
      mediaUrl,
      mediaKind: mediaUrl ? (isVideo(mediaUrl) ? 'video' : 'image') : null,
    };
  }

  private loadShareTargets(): void {
    this.rel.loadFriends().subscribe({
      next: friends => this.friends.set(friends),
      error: () => this.friends.set([]),
    });
    this.rel.loadBlocked().subscribe({
      next: blocked => this.blockedUsers.set(blocked),
      error: () => this.blockedUsers.set([]),
    });
    this.loadShareChats();
  }

  private loadShareChats(): void {
    this.chatService.getChats().subscribe({
      next: chats =>
        this.shareChats.set(
          chats.map(chat => ({ ...chat, personalMessageCount: chat.personalMessageCount ?? 0 })),
        ),
    });
  }

  private loadShareFriendAvatars(friends: Friend[]): void {
    friends.forEach(friend => {
      if (!friend.profilePicture || this.shareFriendAvatarUrls()[friend.id]) return;
      this.mediaCacheService.resolveMediaUrl(friend.profilePicture, 'image', 'high').subscribe(url => {
        if (!url) return;
        this.shareFriendAvatarUrls.update(urls => ({ ...urls, [friend.id]: url }));
      });
    });
  }

  private loadShareGroupAvatars(chats: Chat[]): void {
    chats
      .filter(chat => chat.groupChat)
      .forEach(chat => {
        if (!chat.groupImage || this.shareGroupAvatarUrls()[chat.chatId]) return;
        this.mediaCacheService.resolveMediaUrl(chat.groupImage, 'image', 'high').subscribe(url => {
          if (!url) return;
          this.shareGroupAvatarUrls.update(urls => ({ ...urls, [chat.chatId]: url }));
        });
      });
  }
}

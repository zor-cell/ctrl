import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  Injector,
  OnDestroy,
  OnInit,
  afterNextRender,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { Subject, finalize, of, switchMap, takeUntil } from 'rxjs';
import { USERNAME_PATTERN } from '../user/validators';
import { jwtDecode } from 'jwt-decode';

import { AuthService } from '../auth/auth.service';
import { CustomJwtPayload } from '../auth/dto/CustomJwtPayload';
import { MediaCacheService } from '../core/services/media-cache.service';
import { parseBackendDate } from '../core/utils/date-time.utils';
import { usernameInitial } from '../core/utils/username-initial.utils';
import { NotificationService } from '../core/services/notification.service';
import { Friend } from '../user/dto/Relationship';
import { RelationshipService } from '../user/relationship.service';
import { UserService } from '../user/user.service';
import { Chat, ChatMessage, ChatParticipant, SharedPost } from './chat.models';
import { ChatRealtimePayload } from './chat-realtime.service';
import { ChatRealtimeBridgeService } from './chat-realtime-bridge.service';
import { ChatUnreadService } from './chat-unread.service';
import { ChatService } from './chat.service';
import { CommentsComponent } from '../feed/components/comments/comments.component';
import { Post } from '../feed/dto/Post';

const REACTION_EMOJIS = ['👍', '❤️', '😂', '😮', '😢', '😡', '🎉', '🔥'];
const MAX_GROUP_MEMBERS = 50;
const SHARED_POST_PREFIX = '[[CTRL_SHARED_POST:';
const SHARED_POST_SUFFIX = ']]';

interface SharedPostPayload {
  postId: string;
  username: string;
  profileUserId?: string | null;
  profilePicture?: string | null;
  caption: string;
  url: string;
  mediaRef?: string | null;
  mediaUrl: string | null;
  mediaKind: 'image' | 'video' | null;
  sharedAt?: string;
  canView?: boolean;
  publicPost?: boolean;
  profileUsername?: string | null;
  unavailableReason?: 'PRIVATE' | 'REMOVED' | null;
}

interface ParsedSharedPost {
  post: SharedPostPayload;
  note: string;
}

type GroupConfirmationAction = 'delete' | 'leave';

@Component({
  selector: 'app-chat-page',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    Button,
    Dialog,
    IconField,
    InputIcon,
    InputText,
    Textarea,
    TooltipModule,
    CommentsComponent,
  ],
  templateUrl: './chat-page.component.html',
  styleUrl: './chat-page.component.scss',
})
export class ChatPageComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly chatService = inject(ChatService);
  private readonly relationshipService = inject(RelationshipService);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly notificationService = inject(NotificationService);
  private readonly chatUnreadService = inject(ChatUnreadService);
  private readonly chatRealtimeBridge = inject(ChatRealtimeBridgeService);
  private readonly userService = inject(UserService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly destroy$ = new Subject<void>();
  private readonly messageListRef = viewChild<ElementRef<HTMLElement>>('messageList');
  private readonly messageListEndRef = viewChild<ElementRef<HTMLElement>>('messageListEnd');

  protected readonly chats = signal<Chat[]>([]);
  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly selectedChat = signal<Chat | null>(null);
  protected readonly searchQuery = signal('');
  protected readonly messageText = signal('');
  protected readonly loadingChats = signal(false);
  protected readonly loadingMessages = signal(false);
  protected readonly sendingMessage = signal(false);
  protected readonly friendDropdownOpen = signal(false);
  protected readonly groupDialogVisible = signal(false);
  protected readonly membersDialogVisible = signal(false);
  protected readonly groupConfirmationAction = signal<GroupConfirmationAction | null>(null);
  protected readonly groupActionInProgress = signal(false);
  protected readonly addMemberSearch = signal('');
  protected readonly selectedAddMemberUsernames = signal<string[]>([]);
  protected readonly addingMembers = signal(false);
  protected readonly reactionPickerMessageId = signal<string | null>(null);
  protected readonly hoveredMessageId = signal<string | null>(null);
  protected readonly loadingMembersDialog = signal(false);
  protected readonly sharedPostDraft = signal<SharedPostPayload | null>(null);
  protected readonly chatPostCommentsVisible = signal(false);
  protected readonly selectedChatPostForComments = signal<Post | null>(null);

  protected readonly reactionEmojis = REACTION_EMOJIS;
  protected readonly maxGroupMembers = MAX_GROUP_MEMBERS;
  protected readonly groupName = signal('');
  protected readonly groupMemberSearch = signal('');
  protected readonly selectedGroupUsernames = signal<string[]>([]);
  protected readonly groupImageFile = signal<File | null>(null);
  protected readonly groupImagePreview = signal<string | null>(null);
  protected readonly avatarUrls = signal<Record<string, string>>({});
  private readonly avatarRefs = signal<Record<string, string | null>>({});
  protected readonly groupImageUrls = signal<Record<string, string>>({});
  protected readonly sharedPostMediaUrls = signal<Record<string, string>>({});
  private readonly failedSharedPostMediaUrls = signal<Record<string, true>>({});
  private readonly pendingSharedPostMediaKeys = new Set<string>();
  protected readonly groupConfirmationTitle = computed(() =>
    this.groupConfirmationAction() === 'delete' ? 'Delete group?' : 'Leave group?'
  );
  protected readonly groupConfirmationMessage = computed(() =>
    this.groupConfirmationAction() === 'delete'
      ? 'This will permanently delete the group for everyone. Are you sure you want to continue?'
      : 'You will lose access to this group chat. Are you sure you want to leave?'
  );
  protected readonly groupConfirmationButtonLabel = computed(() =>
    this.groupConfirmationAction() === 'delete' ? 'Delete group' : 'Leave group'
  );
  private pendingSharedPost:
    | {
        username?: string;
        chatId?: string;
        post: SharedPostPayload;
      }
    | null = null;
  private pendingChatId: string | null = null;

  protected readonly friends = this.relationshipService.friends;
  protected readonly currentUsername = computed(() => {
    const token = this.authService.token();
    if (!token) {
      return null;
    }

    try {
      return jwtDecode<CustomJwtPayload>(token).sub ?? null;
    } catch {
      return null;
    }
  });

  protected readonly filteredFriends = computed(() => {
    const query = this.normalizedSearchQuery();
    return this.friends()
      .filter(friend => this.matchesFriend(friend, query))
      .sort((left, right) => this.compareFriendsByChatAvailability(left, right));
  });

  protected readonly filteredChats = computed(() => {
    const query = this.normalizedSearchQuery();
    return this.chats().filter(chat => this.matchesChat(chat, query));
  });

  protected readonly filteredDirectChats = computed(() =>
    this.dedupeDirectChats(
      this.filteredChats().filter(chat => !chat.groupChat && this.isValidDirectChat(chat)),
    ),
  );

  protected readonly filteredGroupChats = computed(() =>
    this.filteredChats().filter(chat => chat.groupChat),
  );

  protected readonly filteredGroupFriends = computed(() => {
    const query = this.groupMemberSearch().trim().toLowerCase();
    return this.friends().filter(friend => this.matchesFriend(friend, query));
  });

  protected readonly filteredAddMemberFriends = computed(() => {
    const chat = this.selectedChat();
    if (!chat?.groupChat) {
      return [];
    }

    const memberUsernames = new Set(chat.participants.map(participant => participant.username));
    const query = this.addMemberSearch().trim().toLowerCase();

    return this.friends()
      .filter(friend => !memberUsernames.has(friend.username))
      .filter(friend => this.matchesFriend(friend, query));
  });

  protected readonly isGroupNameValid = computed(() => {
    const name = this.groupName().trim();
    return name.length > 0 && USERNAME_PATTERN.test(name);
  });

  protected readonly sortedGroupMembers = computed(() => {
    const chat = this.selectedChat();
    if (!chat) {
      return [];
    }

    return [...chat.participants].sort((left, right) => {
      const leftIsOwner = this.isGroupOwner(left, chat);
      const rightIsOwner = this.isGroupOwner(right, chat);
      if (leftIsOwner !== rightIsOwner) {
        return leftIsOwner ? -1 : 1;
      }

      const leftLabel = (left.name || left.username).toLowerCase();
      const rightLabel = (right.name || right.username).toLowerCase();
      return leftLabel.localeCompare(rightLabel);
    });
  });

  constructor() {
    effect(() => {
      const user = this.userService.user();
      if (!user) {
        return;
      }

      queueMicrotask(() => untracked(() => this.loadAvatar(user.id, user.profilePicture)));
    });

    effect(() => {
      const selectedPost = this.selectedChatPostForComments();
      if (!selectedPost?.userId) {
        return;
      }

      const avatarUrl = this.avatarUrls()[selectedPost.userId] ?? '';
      const currentUser = this.userService.user();
      const username =
        currentUser?.id === selectedPost.userId ? currentUser.username : selectedPost.username;

      if (selectedPost.userProfilePic === avatarUrl && selectedPost.username === username) {
        return;
      }

      queueMicrotask(() => untracked(() => {
        const latestPost = this.selectedChatPostForComments();
        if (
          !latestPost
          || latestPost.postId !== selectedPost.postId
          || latestPost.userId !== selectedPost.userId
        ) {
          return;
        }

        this.selectedChatPostForComments.set({
          ...latestPost,
          username,
          userProfilePic: avatarUrl,
        });
      }));
    });
  }

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const username = params.get('user');
        const chatId = params.get('chat');
        const post = this.readSharedPostFromHistory();

        if ((username || chatId) && post) {
          this.pendingSharedPost = { username: username ?? undefined, chatId: chatId ?? undefined, post };
          if (this.chats().length > 0) {
            this.openPendingSharedPostChat();
          }
          return;
        }

        if (chatId) {
          this.pendingChatId = chatId;
          if (this.chats().length > 0) {
            this.openPendingChat();
          }
        }
      });

    this.loadFriends();
    this.loadChats();
    this.chatRealtimeBridge.setMessageHandler(payload => this.handleRealtimeMessage(payload));
    this.chatRealtimeBridge.setChatListHandler(chat => this.handleRealtimeChatSummary(chat));
    this.chatRealtimeBridge.setChatRemovedHandler(chatId => this.handleRealtimeChatRemoved(chatId));
    this.chatRealtimeBridge.ensureConnected();
    void this.syncRealtimeSubscription();
  }

  private async syncRealtimeSubscription(): Promise<void> {
    await this.chatRealtimeBridge.whenConnected();
    const selected = this.selectedChat();
    if (selected) {
      this.chatRealtimeBridge.resyncWatchedChat(selected.chatId);
    }
  }

  ngOnDestroy(): void {
    this.chatRealtimeBridge.setMessageHandler(null);
    this.chatRealtimeBridge.setChatListHandler(null);
    this.chatRealtimeBridge.setChatRemovedHandler(null);
    this.chatRealtimeBridge.unwatchChat();
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onSearchChange(query: string): void {
    this.searchQuery.set(query);
    this.friendDropdownOpen.set(true);
  }

  protected onSearchFocus(): void {
    this.friendDropdownOpen.set(true);
  }

  protected onSearchBlur(): void {
    window.setTimeout(() => this.friendDropdownOpen.set(false), 120);
  }

  protected keepFriendDropdownOpen(event: MouseEvent): void {
    event.preventDefault();
  }

  protected startOrOpenChat(friend: Friend): void {
    const existingChat = this.directChatFor(friend);
    if (existingChat) {
      this.searchQuery.set('');
      this.friendDropdownOpen.set(false);
      this.selectChat(existingChat);
      return;
    }

    this.chatService.createChat({ usernames: [friend.username] })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: chat => {
          this.searchQuery.set('');
          this.friendDropdownOpen.set(false);
          this.upsertChat(chat);
          this.selectChat(chat);
        },
        error: () => this.notificationService.showError('Failed to create chat'),
      });
  }

  protected directChatFor(friend: Friend): Chat | null {
    const matches = this.chats().filter(chat =>
      !chat.groupChat
      && chat.participants.some(participant => participant.username === friend.username)
    );

    return this.pickLatestDirectChat(matches);
  }

  private compareFriendsByChatAvailability(left: Friend, right: Friend): number {
    const leftHasChat = this.directChatFor(left) !== null;
    const rightHasChat = this.directChatFor(right) !== null;
    if (leftHasChat !== rightHasChat) {
      return leftHasChat ? 1 : -1;
    }

    return this.friendDisplayLabel(left).localeCompare(this.friendDisplayLabel(right));
  }

  private friendDisplayLabel(friend: Friend): string {
    return (friend.name || friend.username).toLowerCase();
  }

  protected selectChat(chat: Chat): void {
    const normalizedChat = this.normalizeChat(chat);
    this.friendDropdownOpen.set(false);
    this.reactionPickerMessageId.set(null);
    this.messages.set([]);
    this.selectedChat.set(normalizedChat);
    this.chatRealtimeBridge.watchChat(normalizedChat.chatId);
    this.loadMessages(normalizedChat.chatId);
  }

  protected openGroupDialog(): void {
    this.groupDialogVisible.set(true);
  }

  protected closeGroupDialog(): void {
    this.onGroupDialogVisibilityChange(false);
  }

  protected onGroupDialogVisibilityChange(visible: boolean): void {
    this.groupDialogVisible.set(visible);
    if (!visible) {
      this.groupName.set('');
      this.groupMemberSearch.set('');
      this.selectedGroupUsernames.set([]);
      this.clearGroupImageSelection();
    }
  }

  protected onGroupImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !file.type.startsWith('image/')) {
      return;
    }

    this.revokeGroupImagePreview();
    this.groupImageFile.set(file);
    this.groupImagePreview.set(URL.createObjectURL(file));
    input.value = '';
  }

  protected clearGroupImageSelection(): void {
    this.revokeGroupImagePreview();
    this.groupImageFile.set(null);
  }

  protected openMembersDialog(): void {
    const chat = this.selectedChat();
    if (!chat?.groupChat) {
      return;
    }

    this.resetAddMemberSelection();
    this.membersDialogVisible.set(true);
    this.refreshSelectedGroupChat(chat.chatId);
  }

  protected setHoveredMessage(messageId: string): void {
    this.hoveredMessageId.set(messageId);
  }

  protected clearHoveredMessage(messageId: string): void {
    if (this.hoveredMessageId() === messageId) {
      this.hoveredMessageId.set(null);
    }
  }

  protected canRemoveMember(participant: ChatParticipant, chat: Chat): boolean {
    if (!chat.owner || !chat.groupChat) {
      return false;
    }

    if (this.isGroupOwner(participant, chat)) {
      return false;
    }

    const currentUsername = this.currentUsername();
    return currentUsername ? participant.username !== currentUsername : false;
  }

  protected removeMemberFromGroup(participant: ChatParticipant): void {
    const chat = this.selectedChat();
    if (!chat?.groupChat || !chat.owner) {
      return;
    }

    const previousChat = chat;
    const optimisticChat = this.upsertChat({
      ...chat,
      participants: chat.participants.filter(
        member => this.normalizeId(member.id) !== this.normalizeId(participant.id)
      ),
    });
    this.selectedChat.set(optimisticChat);

    this.chatService.removeMember(chat.chatId, participant.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: updatedChat => {
          const normalizedChat = this.upsertChat(updatedChat);
          if (this.selectedChat()?.chatId === normalizedChat.chatId) {
            this.selectedChat.set(normalizedChat);
          }
          this.loadMessages(normalizedChat.chatId);
          this.notificationService.showSuccess('Member removed');
        },
        error: () => {
          const restoredChat = this.upsertChat(previousChat);
          if (this.selectedChat()?.chatId === restoredChat.chatId) {
            this.selectedChat.set(restoredChat);
          }
          this.notificationService.showError('Failed to remove member');
        },
      });
  }

  protected closeMembersDialog(): void {
    this.membersDialogVisible.set(false);
    this.resetAddMemberSelection();
  }

  protected onMembersDialogVisibilityChange(visible: boolean): void {
    this.membersDialogVisible.set(visible);
    if (!visible) {
      this.resetAddMemberSelection();
    }
  }

  protected requestDeleteSelectedGroup(): void {
    const chat = this.selectedChat();
    if (!chat || !chat.groupChat || !chat.owner) {
      return;
    }

    this.groupConfirmationAction.set('delete');
  }

  protected requestLeaveGroup(): void {
    const chat = this.selectedChat();
    if (!chat?.groupChat || chat.owner) {
      return;
    }

    this.groupConfirmationAction.set('leave');
  }

  protected onGroupConfirmationVisibilityChange(visible: boolean): void {
    if (!visible) {
      this.closeGroupConfirmation();
    }
  }

  protected closeGroupConfirmation(): void {
    if (this.groupActionInProgress()) {
      return;
    }

    this.groupConfirmationAction.set(null);
  }

  protected confirmGroupAction(): void {
    const action = this.groupConfirmationAction();
    if (action === 'delete') {
      this.deleteSelectedGroup();
      return;
    }

    if (action === 'leave') {
      this.leaveGroup();
    }
  }

  protected isAddMemberFriendSelected(username: string): boolean {
    return this.selectedAddMemberUsernames().includes(username);
  }

  protected toggleAddMemberFriend(username: string): void {
    this.selectedAddMemberUsernames.update(usernames =>
      usernames.includes(username)
        ? usernames.filter(selectedUsername => selectedUsername !== username)
        : this.canSelectAdditionalMember()
          ? [...usernames, username]
          : usernames
    );
  }

  protected canSelectAdditionalMember(): boolean {
    const chat = this.selectedChat();
    if (!chat?.groupChat) {
      return false;
    }

    return chat.participants.length + this.selectedAddMemberUsernames().length < MAX_GROUP_MEMBERS;
  }

  protected canAddMembersToGroup(): boolean {
    const chat = this.selectedChat();
    const selectedCount = this.selectedAddMemberUsernames().length;
    return Boolean(
      chat?.groupChat
      && selectedCount > 0
      && chat.participants.length + selectedCount <= MAX_GROUP_MEMBERS
      && !this.addingMembers()
    );
  }

  protected addMembersToGroup(): void {
    const chat = this.selectedChat();
    const usernames = this.selectedAddMemberUsernames();
    if (!chat?.groupChat || !this.canAddMembersToGroup()) {
      return;
    }

    this.addingMembers.set(true);
    this.chatService.addMembers(chat.chatId, usernames)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.addingMembers.set(false))
      )
      .subscribe({
        next: updatedChat => {
          this.resetAddMemberSelection();
          const normalizedChat = this.upsertChat(updatedChat);
          this.selectedChat.set(normalizedChat);
          this.loadMessages(normalizedChat.chatId);
        },
        error: () => this.notificationService.showError('Failed to add members'),
      });
  }

  protected isGroupOwner(participant: ChatParticipant, chat: Chat): boolean {
    const owner = chat.groupOwner;
    if (!owner) {
      return false;
    }

    return this.normalizeId(owner.id) === this.normalizeId(participant.id);
  }

  protected isFriendParticipant(participant: ChatParticipant): boolean {
    const participantUsername = participant.username.toLowerCase();
    const participantId = this.normalizeId(participant.id);
    return this.friends().some(friend =>
      friend.username.toLowerCase() === participantUsername
      || this.normalizeId(friend.id) === participantId
    );
  }

  protected openParticipantProfile(participant: ChatParticipant): void {
    this.router.navigate(['/profile', participant.username]);
  }

  protected groupImageUrl(chat: Chat): string | null {
    return this.groupImageUrls()[chat.chatId] ?? null;
  }

  protected otherParticipant(chat: Chat): ChatParticipant | null {
    if (chat.groupChat || chat.participants.length === 0) {
      return null;
    }

    return this.findOtherParticipant(chat.participants);
  }

  private findOtherParticipant(participants: ChatParticipant[]): ChatParticipant | null {
    const currentUserId = this.userService.user()?.id;
    const currentUsername = this.currentUsername()?.toLowerCase();
    const others = participants.filter(
      participant => !this.isCurrentParticipant(participant, currentUserId, currentUsername),
    );

    return others[0] ?? null;
  }

  private isCurrentParticipant(
    participant: ChatParticipant,
    currentUserId: string | undefined,
    currentUsername: string | undefined,
  ): boolean {
    if (currentUserId && this.normalizeId(participant.id) === this.normalizeId(currentUserId)) {
      return true;
    }

    return !!currentUsername && participant.username.toLowerCase() === currentUsername;
  }

  protected chatDisplayName(chat: Chat): string {
    if (chat.groupChat) {
      return chat.name;
    }

    const other = this.otherParticipant(chat);
    if (other) {
      return other.name?.trim() || other.username;
    }

    return 'Unknown chat';
  }

  protected directChatSubtitle(chat: Chat): string | null {
    const other = this.otherParticipant(chat);
    return other?.name ?? null;
  }

  protected openDirectChatProfile(chat: Chat): void {
    const other = this.otherParticipant(chat);
    if (!other) {
      return;
    }

    this.router.navigate(['/profile', other.username]);
  }

  protected chatAvatarLabel(chat: Chat): string {
    const other = this.otherParticipant(chat);
    if (other) {
      return usernameInitial(other.username);
    }

    return chat.name.charAt(0).toUpperCase();
  }

  protected isGroupFriendSelected(username: string): boolean {
    return this.selectedGroupUsernames().includes(username);
  }

  protected toggleGroupFriend(username: string): void {
    this.selectedGroupUsernames.update(usernames =>
      usernames.includes(username)
        ? usernames.filter(selectedUsername => selectedUsername !== username)
        : usernames.length + 1 < MAX_GROUP_MEMBERS
          ? [...usernames, username]
          : usernames
    );
  }

  protected canSelectGroupFriend(username: string): boolean {
    return this.isGroupFriendSelected(username)
      || this.selectedGroupUsernames().length + 1 < MAX_GROUP_MEMBERS;
  }

  protected selectedGroupMemberCount(): number {
    return this.selectedGroupUsernames().length + 1;
  }

  protected selectedAddMemberCount(chat: Chat): number {
    return chat.participants.length + this.selectedAddMemberUsernames().length;
  }

  protected canCreateGroup(): boolean {
    return this.isGroupNameValid()
      && this.selectedGroupUsernames().length > 0
      && this.selectedGroupMemberCount() <= MAX_GROUP_MEMBERS;
  }

  protected createGroup(): void {
    const name = this.groupName().trim();
    const usernames = this.selectedGroupUsernames();
    const imageFile = this.groupImageFile();
    if (!this.canCreateGroup() || usernames.length === 0) {
      return;
    }

    this.chatService.createChat({ name, usernames })
      .pipe(
        switchMap(chat =>
          imageFile
            ? this.chatService.uploadGroupImage(chat.chatId, imageFile)
            : of(chat)
        ),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: chat => {
          this.closeGroupDialog();
          this.upsertChat(chat);
          this.selectChat(chat);
        },
        error: () => this.notificationService.showError('Failed to create group'),
      });
  }

  protected deleteSelectedGroup(): void {
    const chat = this.selectedChat();
    if (!chat || !chat.groupChat || !chat.owner) {
      return;
    }

    this.groupActionInProgress.set(true);
    this.chatService.deleteChat(chat.chatId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.groupActionInProgress.set(false))
      )
      .subscribe({
        next: () => {
          this.groupConfirmationAction.set(null);
          this.removeChatFromState(chat.chatId);
        },
        error: () => this.notificationService.showError('Failed to delete group'),
      });
  }

  protected leaveGroup(): void {
    const chat = this.selectedChat();
    if (!chat?.groupChat || chat.owner) {
      return;
    }

    this.groupActionInProgress.set(true);
    this.chatService.leaveChat(chat.chatId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.groupActionInProgress.set(false))
      )
      .subscribe({
        next: () => {
          this.groupConfirmationAction.set(null);
          this.removeChatFromState(chat.chatId);
        },
        error: () => this.notificationService.showError('Failed to leave group'),
      });
  }

  protected deleteDirectChatForSelf(): void {
    const chat = this.selectedChat();
    if (!chat || chat.groupChat) {
      return;
    }

    this.chatService.leaveChat(chat.chatId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.removeChatFromState(chat.chatId);
        },
        error: () => this.notificationService.showError('Failed to delete chat'),
      });
  }

  protected isSystemMessage(message: ChatMessage): boolean {
    return message.system;
  }

  protected systemMessageLabel(message: ChatMessage): string {
    if (!message.system) {
      return message.content;
    }

    if (message.systemEvent && message.systemTargets) {
      return this.formatSystemMessage(message);
    }

    return message.content;
  }

  private formatSystemMessage(message: ChatMessage): string {
    const actorLabel = message.own ? 'You' : this.participantLabel(message.sender);
    const currentUserId = this.userService.user()?.id;
    const currentUserIsTarget = currentUserId
      ? message.systemTargets.some(
          target => this.normalizeId(target.id) === this.normalizeId(currentUserId),
        )
      : false;

    switch (message.systemEvent) {
      case 'MEMBER_ADDED':
        if (message.own) {
          return `You added ${this.formatTargetList(message.systemTargets)}`;
        }
        if (currentUserIsTarget && message.systemTargets.length === 1) {
          return `${actorLabel} added you`;
        }
        return `${actorLabel} added ${this.formatTargetList(message.systemTargets)}`;
      case 'MEMBER_REMOVED': {
        const removed = message.systemTargets.length > 0
          ? this.formatTargetList(message.systemTargets)
          : 'a member';
        return message.own
          ? `You removed ${removed}`
          : `${actorLabel} removed ${removed}`;
      }
      case 'MEMBER_LEFT':
        return message.own
          ? 'You left the group'
          : `${actorLabel} left the group`;
      default:
        return message.content;
    }
  }

  private participantLabel(participant: ChatParticipant): string {
    return participant.name || participant.username;
  }

  private formatTargetList(targets: ChatParticipant[]): string {
    if (targets.length === 0) {
      return 'members';
    }

    const currentUserId = this.userService.user()?.id;
    const labels = targets.map(target => {
      if (currentUserId && this.normalizeId(target.id) === this.normalizeId(currentUserId)) {
        return 'you';
      }
      return this.participantLabel(target);
    });

    if (labels.length === 1) {
      return labels[0];
    }
    if (labels.length === 2) {
      return `${labels[0]} and ${labels[1]}`;
    }

    return `${labels.slice(0, -1).join(', ')} and ${labels[labels.length - 1]}`;
  }

  protected toggleReaction(message: ChatMessage, emoji: string): void {
    const chat = this.selectedChat();
    if (!chat || message.system) {
      return;
    }

    this.chatService.reactToMessage(chat.chatId, message.messageId, emoji)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: updatedMessage => {
          this.reactionPickerMessageId.set(null);
          this.hoveredMessageId.set(null);
          this.updateMessageInState(updatedMessage);
        },
        error: () => this.notificationService.showError('Failed to react to message'),
      });
  }

  protected hasUnreadMessages(chat: Chat): boolean {
    return chat.unreadCount > 0 && !this.isSelected(chat);
  }

  protected sendMessage(): void {
    const chat = this.selectedChat();
    const note = this.messageText().trim();
    const sharedPost = this.sharedPostDraft();
    if (!chat || (!note && !sharedPost) || this.sendingMessage()) {
      return;
    }

    this.sendingMessage.set(true);
    this.messageText.set('');
    this.sharedPostDraft.set(null);

    this.chatService.sendMessage(chat.chatId, {
      content: note,
      postIdToShare: sharedPost?.postId ?? null,
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.sendingMessage.set(false))
      )
      .subscribe({
        next: message => {
          const normalizedMessage = this.normalizeMessage(message);
          this.loadSharedPostMedia(normalizedMessage.sharedPost);
          this.appendMessageIfMissing(normalizedMessage);
          this.patchChatLastMessage(chat, normalizedMessage);
        },
        error: () => {
          this.messageText.set(note);
          this.sharedPostDraft.set(sharedPost);
          this.notificationService.showError('Failed to send message');
        },
      });
  }

  protected sendMessageOnEnter(event: KeyboardEvent): void {
    const isEnterKey = event.key === 'Enter' || event.code === 'Enter' || event.code === 'NumpadEnter';
    if (!isEnterKey || event.shiftKey) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    this.sendMessage();
  }

  protected clearSharedPostDraft(): void {
    this.sharedPostDraft.set(null);
  }

  protected sharedPostFromMessage(message: ChatMessage): ParsedSharedPost | null {
    if (message.sharedPost) {
      const parsed = {
        post: this.sharedPostPayloadFromResponse(message.sharedPost),
        note: message.content,
      };
      this.ensureSharedPostPayloadMedia(parsed.post);
      return parsed;
    }

    const parsed = this.parseSharedPostContent(message.content);
    if (parsed) {
      this.ensureSharedPostPayloadMedia(parsed.post);
    }
    return parsed;
  }

  protected hasSharedPost(message: ChatMessage): boolean {
    return message.sharedPost !== null || message.content.startsWith(SHARED_POST_PREFIX);
  }

  protected canViewSharedPost(sharedPost: ParsedSharedPost): boolean {
    return sharedPost.post.canView !== false;
  }

  protected sharedPostUnavailableIcon(sharedPost: ParsedSharedPost): string {
    return sharedPost.post.unavailableReason === 'REMOVED' ? 'pi pi-eye-slash' : 'pi pi-lock';
  }

  protected sharedPostUnavailableTitle(sharedPost: ParsedSharedPost): string {
    return sharedPost.post.unavailableReason === 'REMOVED'
      ? 'Post unavailable'
      : `Private post from ${this.sharedPostAuthorName(sharedPost)}`;
  }

  protected sharedPostUnavailableMessage(sharedPost: ParsedSharedPost): string {
    return sharedPost.post.unavailableReason === 'REMOVED'
      ? 'This post was removed after review and can no longer be viewed.'
      : 'You cannot view this private post because you are not friends with this account.';
  }

  protected sharedPostAuthorName(sharedPost: ParsedSharedPost): string {
    return this.formatSharedPostUsername(this.sharedPostAuthorUsername(sharedPost.post));
  }

  protected sharedPostAuthorInitial(sharedPost: ParsedSharedPost): string {
    return usernameInitial(this.sharedPostAuthorUsername(sharedPost.post));
  }

  protected sharedPostAuthorAvatarUrl(sharedPost: ParsedSharedPost): string | null {
    const profileUserId = this.sharedPostAuthorUserId(sharedPost.post);
    return profileUserId ? this.avatarUrl(profileUserId) : null;
  }

  protected sharedPostMediaUrl(sharedPost: ParsedSharedPost): string | null {
    const mediaUrl = this.resolvedSharedPostMediaUrl(sharedPost.post) ?? sharedPost.post.mediaUrl;
    return mediaUrl && !this.failedSharedPostMediaUrls()[mediaUrl] ? mediaUrl : null;
  }

  protected onSharedPostMediaError(sharedPost: ParsedSharedPost, mediaUrl: string): void {
    this.failedSharedPostMediaUrls.update(urls => ({ ...urls, [mediaUrl]: true }));
    this.ensureSharedPostPayloadMedia(sharedPost.post);
  }

  protected openSharedPostDetail(sharedPost: ParsedSharedPost): void {
    if (!this.canViewSharedPost(sharedPost)) {
      return;
    }

    const postId = this.resolveSharedPostId(sharedPost.post.postId);
    if (!postId) {
      this.notificationService.showError('This post cannot be opened');
      return;
    }

    this.selectedChatPostForComments.set(this.sharedPostToFeedPost(sharedPost));
    this.chatPostCommentsVisible.set(true);
    this.ensureSelectedChatPostAvatar(sharedPost);
    this.ensureSelectedChatPostMedia(sharedPost);
  }

  protected onChatPostCommentsVisibleChange(visible: boolean): void {
    this.chatPostCommentsVisible.set(visible);
    if (!visible) {
      this.selectedChatPostForComments.set(null);
    }
  }

  protected sharedPostProfileRoute(sharedPost: ParsedSharedPost): string[] | null {
    const profileUsername = this.sharedPostProfileUsername(sharedPost.post);
    return profileUsername ? ['/profile', profileUsername] : null;
  }

  protected messagePreview(content: string): string {
    return this.messagePreviewForContent(content);
  }

  protected messagePreviewForMessage(message: ChatMessage): string {
    if (message.sharedPost) {
      return message.content || `Shared post from ${this.sharedPostAuthorName({
        post: this.sharedPostPayloadFromResponse(message.sharedPost),
        note: '',
      })}`;
    }

    return this.messagePreviewForContent(message.content);
  }

  private messagePreviewForContent(content: string): string {
    const sharedPost = this.parseSharedPostContent(content);
    if (!sharedPost) {
      return content;
    }

    return sharedPost.note
      ? sharedPost.note
      : `Shared post from ${this.sharedPostAuthorName(sharedPost)}`;
  }

  protected avatarUrl(id: string): string | null {
    return this.avatarUrls()[id] ?? null;
  }

  protected initialFor(user: ChatParticipant | Friend): string {
    return usernameInitial(user.username);
  }

  private sharedPostAuthorUsername(post: SharedPostPayload): string {
    const currentUser = this.userService.user();
    if (currentUser && this.isCurrentUserSharedPostAuthor(post, currentUser)) {
      return currentUser.username;
    }

    return post.profileUsername ?? post.username;
  }

  private sharedPostProfileUsername(post: SharedPostPayload): string | null {
    const currentUser = this.userService.user();
    if (currentUser && this.isCurrentUserSharedPostAuthor(post, currentUser)) {
      return currentUser.username;
    }

    return post.profileUsername ?? null;
  }

  private sharedPostAuthorUserId(post: SharedPostPayload): string | null {
    const currentUser = this.userService.user();
    if (currentUser && this.isCurrentUserSharedPostAuthor(post, currentUser)) {
      return currentUser.id;
    }

    return post.profileUserId ?? null;
  }

  private isCurrentUserSharedPostAuthor(
    post: SharedPostPayload,
    currentUser: { id: string; username: string },
  ): boolean {
    if (post.profileUserId) {
      return post.profileUserId === currentUser.id;
    }

    const currentUsername = currentUser.username.toLowerCase();
    return [post.profileUsername, post.username]
      .filter((username): username is string => typeof username === 'string')
      .some(username => username.replace(/^@/, '').toLowerCase() === currentUsername);
  }

  private formatSharedPostUsername(username: string): string {
    const trimmed = username.trim();
    if (!trimmed) {
      return '@unknown';
    }

    return trimmed.startsWith('@') ? trimmed : `@${trimmed}`;
  }

  private sharedPostToFeedPost(sharedPost: ParsedSharedPost): Post {
    const post = sharedPost.post;
    const mediaUrl = this.resolvedSharedPostMediaUrl(post) ?? post.mediaUrl;
    const authorUserId = this.sharedPostAuthorUserId(post);

    return {
      postId: post.postId,
      userId: authorUserId ?? '',
      username: this.sharedPostAuthorUsername(post),
      userProfilePic: authorUserId ? this.avatarUrl(authorUserId) ?? '' : '',
      createdAt: post.sharedAt ?? new Date().toISOString(),
      caption: post.caption,
      isPublic: post.publicPost ?? true,
      likes: 0,
      upvotes: 0,
      downvotes: 0,
      userVote: null,
      labelIds: [],
      topicIds: [],
      videoImageRef: mediaUrl ? [mediaUrl] : [],
      isExternal: !post.profileUserId && !post.profileUsername,
      sourceIds: [],
      comments: [],
      explanation: {
        scoreInteraction: 0,
        scoreTopic: 0,
        scoreRecency: 0,
        scorePopularity: 0,
        scoreSource: 0,
        scoreExploration: 0,
        totalScore: 0,
      },
    };
  }

  private ensureSelectedChatPostMedia(sharedPost: ParsedSharedPost): void {
    const post = sharedPost.post;
    if (post.mediaUrl || !post.mediaRef) {
      return;
    }

    this.mediaCacheService.resolveMediaUrl(post.mediaRef, post.mediaKind ?? 'image', 'normal')
      .pipe(takeUntil(this.destroy$))
      .subscribe(url => {
        const selectedPost = this.selectedChatPostForComments();
        if (!url || !selectedPost || selectedPost.postId !== post.postId) {
          return;
        }

        this.selectedChatPostForComments.set({
          ...selectedPost,
          videoImageRef: [url],
        });
      });
  }

  private ensureSelectedChatPostAvatar(sharedPost: ParsedSharedPost): void {
    const profileUserId = this.sharedPostAuthorUserId(sharedPost.post);
    if (!profileUserId) {
      return;
    }

    const currentUser = this.userService.user();
    const profilePicture =
      currentUser?.id === profileUserId
        ? currentUser.profilePicture
        : sharedPost.post.profilePicture ?? null;

    this.loadAvatar(profileUserId, profilePicture);
  }

  protected participantText(chat: Chat): string {
    const count = chat.participants.length;
    return count > 2 ? `${count} members` : '';
  }

  protected isOwnMessage(message: ChatMessage): boolean {
    const username = this.currentUsername();
    return username ? message.sender.username === username : message.own;
  }

  protected isSelected(chat: Chat): boolean {
    return this.selectedChat()?.chatId === chat.chatId;
  }

  private loadFriends(): void {
    this.relationshipService.loadFriends()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: friends => friends.forEach(friend => this.loadAvatar(friend.id, friend.profilePicture)),
        error: () => this.notificationService.showError('Failed to load friends'),
      });
  }

  private loadChats(): void {
    this.loadingChats.set(true);
    this.chatService.getChats()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingChats.set(false))
      )
      .subscribe({
        next: chats => {
          const normalizedChats = chats.map(chat => this.normalizeChat(chat));
          const dedupedChats = this.applyChatListDedupe(normalizedChats);
          this.chats.set(dedupedChats);
          this.chatUnreadService.syncFromChats(dedupedChats);
          dedupedChats.forEach(chat => {
            chat.participants.forEach(participant =>
              this.loadAvatar(participant.id, participant.profilePicture)
            );
            if (chat.groupOwner) {
              this.loadAvatar(chat.groupOwner.id, chat.groupOwner.profilePicture);
            }
            this.loadGroupImage(chat);
          });

          const selectedChat = this.selectedChat();
          if (selectedChat) {
            const refreshedChat = dedupedChats.find(chat => chat.chatId === selectedChat.chatId);
            if (refreshedChat) {
              this.selectedChat.set(refreshedChat);
            }
          }

          this.openPendingSharedPostChat();
          this.openPendingChat();
        },
        error: () => this.notificationService.showError('Failed to load chats'),
      });
  }

  private openPendingSharedPostChat(): void {
    const pending = this.pendingSharedPost;
    if (!pending) {
      return;
    }

    const existingChat = pending.chatId
      ? this.chats().find(chat => chat.chatId === pending.chatId) ?? null
      : pending.username
        ? this.directChatForUsername(pending.username)
        : null;
    if (existingChat) {
      this.pendingSharedPost = null;
      this.selectChat(existingChat);
      this.sharedPostDraft.set(pending.post);
      this.messageText.set('');
      this.clearShareQueryParams();
      return;
    }

    if (!pending.username) {
      this.pendingSharedPost = null;
      this.notificationService.showError('Failed to open chat');
      this.clearShareQueryParams();
      return;
    }

    this.pendingSharedPost = null;
    this.chatService.createChat({ usernames: [pending.username] })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: chat => {
          this.upsertChat(chat);
          this.selectChat(chat);
          this.sharedPostDraft.set(pending.post);
          this.messageText.set('');
          this.clearShareQueryParams();
        },
        error: () => {
          this.notificationService.showError('Failed to open chat');
          this.clearShareQueryParams();
        },
      });
  }

  private openPendingChat(): void {
    const chatId = this.pendingChatId;
    if (!chatId || this.pendingSharedPost) {
      return;
    }

    const chat = this.chats().find(existingChat => existingChat.chatId === chatId);
    this.pendingChatId = null;
    if (!chat) {
      this.clearChatQueryParam();
      return;
    }

    this.selectChat(chat);
    this.clearChatQueryParam();
  }

  private directChatForUsername(username: string): Chat | null {
    const normalizedUsername = username.toLowerCase();
    const matches = this.chats().filter(chat =>
      !chat.groupChat
      && chat.participants.some(
        participant => participant.username.toLowerCase() === normalizedUsername
      )
    );

    return this.pickLatestDirectChat(matches);
  }

  private clearShareQueryParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { user: null, chat: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private clearChatQueryParam(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { chat: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private loadMessages(chatId: string): void {
    this.loadingMessages.set(true);
    this.chatService.getMessages(chatId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingMessages.set(false))
      )
      .subscribe({
        next: messages => {
          if (this.selectedChat()?.chatId !== chatId) {
            return;
          }

          const normalizedMessages = messages.map(message => this.normalizeMessage(message));
          this.messages.update(existingMessages =>
            this.mergeMessages(normalizedMessages, existingMessages)
          );
          normalizedMessages.forEach(normalizedMessage => {
            this.loadAvatar(normalizedMessage.sender.id, normalizedMessage.sender.profilePicture);
            this.loadSharedPostMedia(normalizedMessage.sharedPost);
          });
          this.markChatReadInState(chatId);
          this.scrollToLatestMessage();
        },
        error: () => this.notificationService.showError('Failed to load messages'),
      });
  }

  private upsertChat(nextChat: Chat): Chat {
    const normalizedChat = this.normalizeChat(nextChat);
    this.chats.update(chats => {
      const otherChats = chats.filter(chat => chat.chatId !== normalizedChat.chatId);
      return this.applyChatListDedupe([normalizedChat, ...otherChats]);
    });

    normalizedChat.participants.forEach(participant =>
      this.loadAvatar(participant.id, participant.profilePicture)
    );
    if (normalizedChat.groupOwner) {
      this.loadAvatar(normalizedChat.groupOwner.id, normalizedChat.groupOwner.profilePicture);
    }
    this.loadGroupImage(normalizedChat);
    this.loadSharedPostMedia(normalizedChat.lastMessage?.sharedPost ?? null);
    return normalizedChat;
  }

  private loadGroupImage(chat: Chat): void {
    if (!chat.groupImage || this.groupImageUrls()[chat.chatId]) {
      return;
    }

    this.mediaCacheService.resolveMediaUrl(chat.groupImage, 'image', 'high')
      .pipe(takeUntil(this.destroy$))
      .subscribe(url => {
        if (!url) {
          return;
        }
        this.groupImageUrls.update(urls => ({ ...urls, [chat.chatId]: url }));
      });
  }

  private revokeGroupImagePreview(): void {
    const preview = this.groupImagePreview();
    if (preview) {
      URL.revokeObjectURL(preview);
    }
    this.groupImagePreview.set(null);
  }

  private loadAvatar(id: string, profilePicture: string | null): void {
    const previousRef = this.avatarRefs()[id];
    if (previousRef === profilePicture && (!profilePicture || this.avatarUrls()[id])) {
      return;
    }

    this.avatarRefs.update(refs => ({ ...refs, [id]: profilePicture }));
    this.avatarUrls.update(urls => {
      const remaining = { ...urls };
      delete remaining[id];
      return remaining;
    });

    if (!profilePicture) {
      return;
    }

    this.mediaCacheService.resolveMediaUrl(profilePicture, 'image', 'high')
      .pipe(takeUntil(this.destroy$))
      .subscribe(url => {
        if (!url || this.avatarRefs()[id] !== profilePicture) {
          return;
        }
        this.avatarUrls.update(urls => ({ ...urls, [id]: url }));
      });
  }

  private normalizedSearchQuery(): string {
    return this.searchQuery().trim().toLowerCase();
  }

  private matchesFriend(friend: Friend, query: string): boolean {
    if (!query) {
      return true;
    }

    return friend.username.toLowerCase().includes(query)
      || (friend.name ?? '').toLowerCase().includes(query);
  }

  private removeChatFromState(chatId: string): void {
    if (this.selectedChat()?.chatId === chatId) {
      this.chatRealtimeBridge.unwatchChat();
    }

    this.chats.update(chats => {
      const nextChats = chats.filter(existingChat => existingChat.chatId !== chatId);
      this.chatUnreadService.syncFromChats(nextChats);
      return nextChats;
    });
    this.messages.set([]);
    this.selectedChat.set(null);
  }

  private markChatReadInState(chatId: string): void {
    this.chats.update(chats => {
      const nextChats = chats.map(chat =>
        chat.chatId === chatId ? { ...chat, unreadCount: 0 } : chat
      );
      this.chatUnreadService.syncFromChats(nextChats);
      return nextChats;
    });

    const selectedChat = this.selectedChat();
    if (selectedChat?.chatId === chatId) {
      this.selectedChat.set({ ...selectedChat, unreadCount: 0 });
    }
  }

  private normalizeId(id: string): string {
    return id.trim().toLowerCase();
  }

  private applyChatListDedupe(chats: Chat[]): Chat[] {
    const groups = chats.filter(chat => chat.groupChat);
    const direct = this.dedupeDirectChats(
      chats.filter(chat => !chat.groupChat && this.isValidDirectChat(chat)),
    );
    return [...groups, ...direct];
  }

  private isValidDirectChat(chat: Chat): boolean {
    return this.findOtherParticipant(chat.participants) !== null;
  }

  private appendMessageIfMissing(message: ChatMessage): void {
    let appended = false;
    this.messages.update(messages => {
      if (messages.some(existing => existing.messageId === message.messageId)) {
        return messages;
      }

      appended = true;
      return [...messages, message];
    });

    if (appended) {
      this.scrollToLatestMessage();
    }
  }

  private mergeMessages(
    loadedMessages: ChatMessage[],
    existingMessages: ChatMessage[],
  ): ChatMessage[] {
    const messagesById = new Map<string, ChatMessage>();

    existingMessages.forEach(message => messagesById.set(message.messageId, message));
    loadedMessages.forEach(message => messagesById.set(message.messageId, message));

    return Array.from(messagesById.values()).sort(
      (left, right) => parseBackendDate(left.date).getTime() - parseBackendDate(right.date).getTime(),
    );
  }

  private scrollToLatestMessage(): void {
    afterNextRender(
      () => {
        requestAnimationFrame(() => {
          const list = this.messageListRef()?.nativeElement;
          const end = this.messageListEndRef()?.nativeElement;

          if (end) {
            end.scrollIntoView({ block: 'end' });
          } else if (list) {
            list.scrollTop = list.scrollHeight;
          }
        });
      },
      { injector: this.injector },
    );
  }

  private patchChatLastMessage(chat: Chat, message: ChatMessage): void {
    const updatedChat: Chat = {
      ...chat,
      lastMessage: message,
      unreadCount: 0,
    };

    const normalizedChat = this.upsertChat(updatedChat);

    if (this.selectedChat()?.chatId === chat.chatId) {
      this.selectedChat.set(normalizedChat);
    }
  }

  private directChatPartnerKey(chat: Chat): string {
    const other = this.otherParticipant(chat);
    if (other) {
      return other.username.toLowerCase();
    }

    const lastMessage = chat.lastMessage;
    if (lastMessage && !lastMessage.own) {
      return lastMessage.sender.username.toLowerCase();
    }

    return chat.chatId;
  }

  private dedupeDirectChats(chats: Chat[]): Chat[] {
    const byOtherUser = new Map<string, Chat>();

    for (const chat of chats) {
      const key = this.directChatPartnerKey(chat);
      const existing = byOtherUser.get(key);

      if (!existing || this.isChatMoreRecent(chat, existing)) {
        byOtherUser.set(key, chat);
      }
    }

    return Array.from(byOtherUser.values()).sort(
      (left, right) => this.chatActivityTime(right) - this.chatActivityTime(left),
    );
  }

  private pickLatestDirectChat(chats: Chat[]): Chat | null {
    if (chats.length === 0) {
      return null;
    }

    return chats.reduce((latest, chat) =>
      this.isChatMoreRecent(chat, latest) ? chat : latest,
    );
  }

  private isChatMoreRecent(left: Chat, right: Chat): boolean {
    return this.chatActivityTime(left) > this.chatActivityTime(right);
  }

  private chatActivityTime(chat: Chat): number {
    const date = chat.lastMessage?.date ?? chat.createdAt;
    return parseBackendDate(date).getTime();
  }

  private handleRealtimeMessage(payload: ChatRealtimePayload): void {
    const selected = this.selectedChat();
    if (!selected || selected.chatId !== payload.chatId) {
      return;
    }

    const normalizedMessage = this.normalizeMessage(payload.message);
    if (payload.type === 'MESSAGE') {
      this.appendMessageIfMissing(normalizedMessage);
      this.markChatReadInState(payload.chatId);
    } else {
      this.updateMessageInState(normalizedMessage);
    }

    this.loadSharedPostMedia(normalizedMessage.sharedPost);
    this.loadAvatar(
      normalizedMessage.sender.id,
      normalizedMessage.sender.profilePicture,
    );
  }

  private handleRealtimeChatSummary(chat: Chat): void {
    const selected = this.selectedChat();
    if (selected?.chatId === chat.chatId) {
      const visibleChat = { ...chat, unreadCount: 0 };
      const normalizedChat = this.upsertChat(visibleChat);
      this.selectedChat.set(normalizedChat);

      if (chat.lastMessage) {
        const normalizedMessage = this.normalizeMessage(chat.lastMessage);
        this.loadSharedPostMedia(normalizedMessage.sharedPost);
        this.appendMessageIfMissing(normalizedMessage);
        this.loadAvatar(
          normalizedMessage.sender.id,
          normalizedMessage.sender.profilePicture,
        );
      }
      this.markChatReadInState(chat.chatId);
      return;
    }

    this.upsertChat(chat);
  }

  private handleRealtimeChatRemoved(chatId: string): void {
    this.removeChatFromState(chatId);
  }

  private refreshSelectedGroupChat(chatId: string): void {
    this.loadingMembersDialog.set(true);
    this.chatService.getChat(chatId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loadingMembersDialog.set(false))
      )
      .subscribe({
        next: chat => {
          const normalizedChat = this.upsertChat(chat);
          if (this.selectedChat()?.chatId === chat.chatId) {
            this.selectedChat.set(normalizedChat);
          }
        },
        error: () => this.notificationService.showError('Failed to load group members'),
      });
  }

  private normalizeMessage(message: ChatMessage): ChatMessage {
    return {
      ...message,
      date: this.normalizeBackendDate(message.date),
      system: message.system ?? false,
      systemEvent: message.systemEvent ?? null,
      systemTargets: message.systemTargets ?? [],
      reactions: message.reactions ?? [],
      sharedPost: message.sharedPost ?? null,
    };
  }

  private normalizeChat(chat: Chat): Chat {
    return {
      ...chat,
      createdAt: this.normalizeBackendDate(chat.createdAt),
      lastMessage: chat.lastMessage ? this.normalizeMessage(chat.lastMessage) : null,
      personalMessageCount: chat.personalMessageCount ?? 0,
    };
  }

  private normalizeBackendDate(value: string): string {
    return parseBackendDate(value).toISOString();
  }

  private updateMessageInState(updatedMessage: ChatMessage): void {
    const normalizedMessage = this.normalizeMessage(updatedMessage);
    this.messages.update(messages =>
      messages.map(message =>
        message.messageId === normalizedMessage.messageId ? normalizedMessage : message
      )
    );
  }

  private resetAddMemberSelection(): void {
    this.addMemberSearch.set('');
    this.selectedAddMemberUsernames.set([]);
  }

  private matchesChat(chat: Chat, query: string): boolean {
    if (!query) {
      return true;
    }

    return chat.name.toLowerCase().includes(query)
      || chat.participants.some(participant =>
        participant.username.toLowerCase().includes(query)
        || (participant.name ?? '').toLowerCase().includes(query)
      );
  }

  private readSharedPostFromHistory(): SharedPostPayload | null {
    const state = history.state as { sharedPost?: unknown };
    if (!state?.sharedPost) {
      return null;
    }

    return this.normalizeSharedPostPayload(state.sharedPost);
  }

  private isSharedPostPayload(value: unknown): value is SharedPostPayload {
    if (!value || typeof value !== 'object') {
      return false;
    }

    const candidate = value as SharedPostPayload & { postId?: unknown };
    return this.resolveSharedPostId(candidate.postId) !== null
      && typeof candidate.username === 'string'
      && (
        candidate.profileUserId === undefined
        || candidate.profileUserId === null
        || typeof candidate.profileUserId === 'string'
      )
      && (
        candidate.profilePicture === undefined
        || candidate.profilePicture === null
        || typeof candidate.profilePicture === 'string'
      )
      && typeof candidate.caption === 'string'
      && typeof candidate.url === 'string'
      && (
        candidate.mediaRef === undefined
        || candidate.mediaRef === null
        || typeof candidate.mediaRef === 'string'
      )
      && (candidate.mediaUrl === null || typeof candidate.mediaUrl === 'string')
      && (candidate.mediaKind === null || candidate.mediaKind === 'image' || candidate.mediaKind === 'video')
      && (candidate.sharedAt === undefined || typeof candidate.sharedAt === 'string')
      && (candidate.canView === undefined || typeof candidate.canView === 'boolean')
      && (candidate.publicPost === undefined || typeof candidate.publicPost === 'boolean')
      && (
        candidate.profileUsername === undefined
        || candidate.profileUsername === null
        || typeof candidate.profileUsername === 'string'
      )
      && (
        candidate.unavailableReason === undefined
        || candidate.unavailableReason === null
        || candidate.unavailableReason === 'PRIVATE'
        || candidate.unavailableReason === 'REMOVED'
      );
  }

  private parseSharedPostContent(content: string): ParsedSharedPost | null {
    if (!content.startsWith(SHARED_POST_PREFIX)) {
      return null;
    }

    const markerEnd = content.indexOf(SHARED_POST_SUFFIX);
    if (markerEnd < 0) {
      return null;
    }

    const encodedPost = content.slice(SHARED_POST_PREFIX.length, markerEnd);
    const note = content.slice(markerEnd + SHARED_POST_SUFFIX.length).trimStart();

    try {
      const payload = JSON.parse(decodeURIComponent(encodedPost)) as unknown;
      const sharedPost = this.normalizeSharedPostPayload(payload);
      if (!sharedPost) {
        return null;
      }

      return { post: sharedPost, note };
    } catch {
      return null;
    }
  }

  private normalizeSharedPostPayload(value: unknown): SharedPostPayload | null {
    if (!this.isSharedPostPayload(value)) {
      return null;
    }

    const candidate = value as Partial<SharedPostPayload> & { postId?: unknown };
    const postId = this.resolveSharedPostId(candidate.postId);
    if (!postId) {
      return null;
    }
    const candidateMediaUrl = candidate.mediaUrl ?? null;
    const mediaRef = candidate.mediaRef
      ?? (
        candidateMediaUrl && this.mediaCacheService.isBackendManagedMediaUrl(candidateMediaUrl)
          ? candidateMediaUrl
          : null
      );
    const mediaUrl =
      candidateMediaUrl && !this.mediaCacheService.isBackendManagedMediaUrl(candidateMediaUrl)
        ? candidateMediaUrl
        : null;

    return {
      postId,
      username: candidate.username ?? '',
      profileUserId: candidate.profileUserId ?? null,
      profilePicture: candidate.profilePicture ?? null,
      caption: candidate.caption ?? '',
      url: candidate.url ?? `${window.location.origin}/posts/${encodeURIComponent(postId)}`,
      mediaRef,
      mediaUrl,
      mediaKind: candidate.mediaKind ?? null,
      sharedAt: candidate.sharedAt,
      canView: candidate.canView,
      publicPost: candidate.publicPost,
      profileUsername: candidate.profileUsername,
      unavailableReason: candidate.unavailableReason ?? (candidate.canView === false ? 'PRIVATE' : null),
    };
  }

  private sharedPostPayloadFromResponse(sharedPost: SharedPost): SharedPostPayload {
    const resolvedMediaUrl =
      this.resolvedSharedPostMediaUrlFromParts(sharedPost.postId, sharedPost.mediaRef)
      ?? this.sharedPostMediaUrls()[sharedPost.sharedId]
      ?? null;
    const canView = sharedPost.canView ?? true;
    const postId = this.resolveSharedPostId(sharedPost.postId) ?? '';
    const mediaUrl = canView
      && sharedPost.mediaRef?.startsWith('http')
      && !this.mediaCacheService.isBackendManagedMediaUrl(sharedPost.mediaRef)
      ? sharedPost.mediaRef
      : canView
        ? resolvedMediaUrl
        : null;

    return {
      postId,
      username: sharedPost.username,
      profileUserId: sharedPost.profileUserId,
      profilePicture: sharedPost.profilePicture,
      caption: sharedPost.caption ?? '',
      url: `${window.location.origin}/posts/${encodeURIComponent(postId)}`,
      mediaRef: sharedPost.mediaRef,
      mediaUrl,
      mediaKind: sharedPost.mediaKind,
      sharedAt: sharedPost.sharedAt,
      canView,
      publicPost: sharedPost.publicPost ?? true,
      profileUsername: sharedPost.profileUsername,
      unavailableReason: sharedPost.unavailableReason ?? (canView ? null : 'PRIVATE'),
    };
  }

  private resolveSharedPostId(value: unknown): string | null {
    if (typeof value === 'string') {
      const trimmed = value.trim();
      return trimmed ? trimmed : null;
    }

    if (value && typeof value === 'object') {
      const candidate = value as { postId?: unknown; id?: unknown };
      return this.resolveSharedPostId(candidate.postId ?? candidate.id);
    }

    return null;
  }

  private sharedPostMediaKey(post: SharedPostPayload): string | null {
    return this.sharedPostMediaKeyFromParts(post.postId, post.mediaRef ?? post.mediaUrl);
  }

  private sharedPostMediaKeyFromParts(postId: string | null | undefined, mediaRef: string | null | undefined): string | null {
    if (!postId || !mediaRef) {
      return null;
    }

    return `${postId}:${mediaRef}`;
  }

  private resolvedSharedPostMediaUrl(post: SharedPostPayload): string | null {
    const mediaKey = this.sharedPostMediaKey(post);
    return mediaKey ? this.sharedPostMediaUrls()[mediaKey] ?? null : null;
  }

  private resolvedSharedPostMediaUrlFromParts(
    postId: string | null | undefined,
    mediaRef: string | null | undefined,
  ): string | null {
    const mediaKey = this.sharedPostMediaKeyFromParts(postId, mediaRef);
    return mediaKey ? this.sharedPostMediaUrls()[mediaKey] ?? null : null;
  }

  private ensureSharedPostPayloadMedia(post: SharedPostPayload): void {
    if (!post.mediaRef) {
      return;
    }

    const mediaKey = this.sharedPostMediaKey(post);
    if (!mediaKey || this.sharedPostMediaUrls()[mediaKey] || this.pendingSharedPostMediaKeys.has(mediaKey)) {
      return;
    }

    this.pendingSharedPostMediaKeys.add(mediaKey);
    this.mediaCacheService.resolveMediaUrl(post.mediaRef, post.mediaKind ?? 'image', 'normal')
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.pendingSharedPostMediaKeys.delete(mediaKey))
      )
      .subscribe(url => {
        if (!url) {
          return;
        }

        this.sharedPostMediaUrls.update(urls => ({ ...urls, [mediaKey]: url }));
      });
  }

  private loadSharedPostMedia(sharedPost: SharedPost | null): void {
    if (!sharedPost) {
      return;
    }

    if (sharedPost.profileUserId) {
      this.loadAvatar(sharedPost.profileUserId, sharedPost.profilePicture);
    }

    const mediaKey = this.sharedPostMediaKeyFromParts(sharedPost.postId, sharedPost.mediaRef);
    if (
      !sharedPost.mediaRef
      || this.sharedPostMediaUrls()[sharedPost.sharedId]
      || (mediaKey !== null && this.sharedPostMediaUrls()[mediaKey])
    ) {
      return;
    }

    this.mediaCacheService.resolveMediaUrl(sharedPost.mediaRef, sharedPost.mediaKind ?? 'image', 'normal')
      .pipe(takeUntil(this.destroy$))
      .subscribe(url => {
        if (!url) {
          return;
        }
        this.sharedPostMediaUrls.update(urls => ({
          ...urls,
          [sharedPost.sharedId]: url,
          ...(mediaKey ? { [mediaKey]: url } : {}),
        }));
      });
  }
}

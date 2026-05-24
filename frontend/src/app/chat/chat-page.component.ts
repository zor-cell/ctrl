import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  Injector,
  OnDestroy,
  OnInit,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
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
import { usernameInitial } from '../core/utils/username-initial.utils';
import { NotificationService } from '../core/services/notification.service';
import { Friend } from '../user/dto/Relationship';
import { RelationshipService } from '../user/relationship.service';
import { UserService } from '../user/user.service';
import { Chat, ChatMessage, ChatParticipant } from './chat.models';
import { ChatRealtimePayload } from './chat-realtime.service';
import { ChatRealtimeBridgeService } from './chat-realtime-bridge.service';
import { ChatUnreadService } from './chat-unread.service';
import { ChatService } from './chat.service';

const REACTION_EMOJIS = ['👍', '❤️', '😂', '😮', '😢', '😡', '🎉', '🔥'];

@Component({
  selector: 'app-chat-page',
  imports: [CommonModule, FormsModule, Button, Dialog, IconField, InputIcon, InputText, Textarea, TooltipModule],
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
  protected readonly addMemberSearch = signal('');
  protected readonly selectedAddMemberUsernames = signal<string[]>([]);
  protected readonly addingMembers = signal(false);
  protected readonly reactionPickerMessageId = signal<string | null>(null);
  protected readonly hoveredMessageId = signal<string | null>(null);
  protected readonly loadingMembersDialog = signal(false);

  protected readonly reactionEmojis = REACTION_EMOJIS;
  protected readonly groupName = signal('');
  protected readonly groupMemberSearch = signal('');
  protected readonly selectedGroupUsernames = signal<string[]>([]);
  protected readonly groupImageFile = signal<File | null>(null);
  protected readonly groupImagePreview = signal<string | null>(null);
  protected readonly avatarUrls = signal<Record<string, string>>({});
  protected readonly groupImageUrls = signal<Record<string, string>>({});

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
      .filter(friend => this.matchesFriend(friend, query));
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

  ngOnInit(): void {
    this.loadFriends();
    this.loadChats();
    this.chatRealtimeBridge.setMessageHandler(payload => this.handleRealtimeMessage(payload));
    this.chatRealtimeBridge.setChatListHandler(chat => this.handleRealtimeChatSummary(chat));
  }

  ngOnDestroy(): void {
    this.chatRealtimeBridge.setMessageHandler(null);
    this.chatRealtimeBridge.setChatListHandler(null);
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

  protected selectChat(chat: Chat): void {
    this.friendDropdownOpen.set(false);
    this.reactionPickerMessageId.set(null);
    this.messages.set([]);
    this.selectedChat.set(chat);
    this.chatRealtimeBridge.watchChat(chat.chatId);
    this.loadMessages(chat.chatId);
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

    this.chatService.removeMember(chat.chatId, participant.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.refreshSelectedGroupChat(chat.chatId);
          this.loadMessages(chat.chatId);
          this.notificationService.showSuccess('Member removed');
        },
        error: () => this.notificationService.showError('Failed to remove member'),
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

  protected isAddMemberFriendSelected(username: string): boolean {
    return this.selectedAddMemberUsernames().includes(username);
  }

  protected toggleAddMemberFriend(username: string): void {
    this.selectedAddMemberUsernames.update(usernames =>
      usernames.includes(username)
        ? usernames.filter(selectedUsername => selectedUsername !== username)
        : [...usernames, username]
    );
  }

  protected addMembersToGroup(): void {
    const chat = this.selectedChat();
    const usernames = this.selectedAddMemberUsernames();
    if (!chat?.groupChat || usernames.length === 0 || this.addingMembers()) {
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
          this.upsertChat(updatedChat);
          this.selectedChat.set(updatedChat);
          this.loadMessages(updatedChat.chatId);
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
        : [...usernames, username]
    );
  }

  protected canCreateGroup(): boolean {
    return this.isGroupNameValid() && this.selectedGroupUsernames().length > 0;
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

    this.chatService.deleteChat(chat.chatId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
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

    this.chatService.leaveChat(chat.chatId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
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
    const content = this.messageText().trim();
    if (!chat || !content || this.sendingMessage()) {
      return;
    }

    this.sendingMessage.set(true);
    this.messageText.set('');

    this.chatService.sendMessage(chat.chatId, { content })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.sendingMessage.set(false))
      )
      .subscribe({
        next: message => {
          const normalizedMessage = this.normalizeMessage(message);
          this.appendMessageIfMissing(normalizedMessage);
          this.patchChatLastMessage(chat, normalizedMessage);
        },
        error: () => {
          this.messageText.set(content);
          this.notificationService.showError('Failed to send message');
        },
      });
  }

  protected avatarUrl(id: string): string | null {
    return this.avatarUrls()[id] ?? null;
  }

  protected initialFor(user: ChatParticipant | Friend): string {
    return usernameInitial(user.username);
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
          const dedupedChats = this.applyChatListDedupe(chats);
          this.chats.set(dedupedChats);
          this.chatUnreadService.syncFromChats(dedupedChats);
          chats.forEach(chat => {
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
            const refreshedChat = chats.find(chat => chat.chatId === selectedChat.chatId);
            if (refreshedChat) {
              this.selectedChat.set(refreshedChat);
            }
          }
        },
        error: () => this.notificationService.showError('Failed to load chats'),
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
          this.messages.set(messages.map(message => this.normalizeMessage(message)));
          messages.forEach(message =>
            this.loadAvatar(message.sender.id, message.sender.profilePicture)
          );
          this.markChatReadInState(chatId);
          this.scrollToLatestMessage();
        },
        error: () => this.notificationService.showError('Failed to load messages'),
      });
  }

  private upsertChat(nextChat: Chat): void {
    this.chats.update(chats => {
      const otherChats = chats.filter(chat => chat.chatId !== nextChat.chatId);
      return this.applyChatListDedupe([nextChat, ...otherChats]);
    });

    nextChat.participants.forEach(participant =>
      this.loadAvatar(participant.id, participant.profilePicture)
    );
    if (nextChat.groupOwner) {
      this.loadAvatar(nextChat.groupOwner.id, nextChat.groupOwner.profilePicture);
    }
    this.loadGroupImage(nextChat);
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
    if (!profilePicture || this.avatarUrls()[id]) {
      return;
    }

    this.mediaCacheService.resolveMediaUrl(profilePicture, 'image', 'high')
      .pipe(takeUntil(this.destroy$))
      .subscribe(url => {
        if (!url) {
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

    this.upsertChat(updatedChat);

    if (this.selectedChat()?.chatId === chat.chatId) {
      this.selectedChat.set(updatedChat);
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
    return new Date(date).getTime();
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

    this.loadAvatar(
      normalizedMessage.sender.id,
      normalizedMessage.sender.profilePicture,
    );
  }

  private handleRealtimeChatSummary(chat: Chat): void {
    this.upsertChat(chat);

    const selected = this.selectedChat();
    if (selected?.chatId === chat.chatId) {
      this.selectedChat.set(chat);
      if (chat.unreadCount === 0) {
        this.markChatReadInState(chat.chatId);
      }
    }
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
          this.upsertChat(chat);
          if (this.selectedChat()?.chatId === chat.chatId) {
            this.selectedChat.set(chat);
          }
        },
        error: () => this.notificationService.showError('Failed to load group members'),
      });
  }

  private normalizeMessage(message: ChatMessage): ChatMessage {
    return {
      ...message,
      system: message.system ?? false,
      systemEvent: message.systemEvent ?? null,
      systemTargets: message.systemTargets ?? [],
      reactions: message.reactions ?? [],
    };
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
}

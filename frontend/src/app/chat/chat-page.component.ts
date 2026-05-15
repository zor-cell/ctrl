import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Subject, finalize, takeUntil } from 'rxjs';
import { jwtDecode } from 'jwt-decode';

import { AuthService } from '../auth/auth.service';
import { CustomJwtPayload } from '../auth/dto/CustomJwtPayload';
import { MediaCacheService } from '../core/services/media-cache.service';
import { NotificationService } from '../core/services/notification.service';
import { Friend } from '../user/dto/Relationship';
import { RelationshipService } from '../user/relationship.service';
import { Chat, ChatMessage, ChatParticipant } from './chat.models';
import { ChatService } from './chat.service';

@Component({
  selector: 'app-chat-page',
  imports: [CommonModule, FormsModule, Button, IconField, InputIcon, InputText, Textarea],
  templateUrl: './chat-page.component.html',
  styleUrl: './chat-page.component.scss',
})
export class ChatPageComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly chatService = inject(ChatService);
  private readonly relationshipService = inject(RelationshipService);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroy$ = new Subject<void>();

  protected readonly chats = signal<Chat[]>([]);
  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly selectedChat = signal<Chat | null>(null);
  protected readonly searchQuery = signal('');
  protected readonly messageText = signal('');
  protected readonly loadingChats = signal(false);
  protected readonly loadingMessages = signal(false);
  protected readonly sendingMessage = signal(false);
  protected readonly friendDropdownOpen = signal(false);
  protected readonly groupFormOpen = signal(false);
  protected readonly groupName = signal('');
  protected readonly selectedGroupUsernames = signal<string[]>([]);
  protected readonly avatarUrls = signal<Record<string, string>>({});

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

  ngOnInit(): void {
    this.loadFriends();
    this.loadChats();
  }

  ngOnDestroy(): void {
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
    return this.chats().find(chat =>
      !chat.groupChat
      && chat.participants.some(participant => participant.username === friend.username)
    ) ?? null;
  }

  protected selectChat(chat: Chat): void {
    this.friendDropdownOpen.set(false);
    this.selectedChat.set(chat);
    this.loadMessages(chat.chatId);
  }

  protected toggleGroupForm(): void {
    this.groupFormOpen.update(open => !open);
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
    return this.groupName().trim().length > 0 && this.selectedGroupUsernames().length > 0;
  }

  protected createGroup(): void {
    const name = this.groupName().trim();
    const usernames = this.selectedGroupUsernames();
    if (!name || usernames.length === 0) {
      return;
    }

    this.chatService.createChat({ name, usernames })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: chat => {
          this.groupName.set('');
          this.selectedGroupUsernames.set([]);
          this.groupFormOpen.set(false);
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
          this.chats.update(chats =>
            chats.filter(existingChat => existingChat.chatId !== chat.chatId)
          );
          this.messages.set([]);
          this.selectedChat.set(null);
        },
        error: () => this.notificationService.showError('Failed to delete group'),
      });
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
          this.messages.update(messages => [...messages, message]);
          this.loadChats();
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
    return (user.name?.charAt(0) || user.username.charAt(0)).toUpperCase();
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
          this.chats.set(chats);
          chats.forEach(chat =>
            chat.participants.forEach(participant =>
              this.loadAvatar(participant.id, participant.profilePicture)
            )
          );

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
          this.messages.set(messages);
          messages.forEach(message =>
            this.loadAvatar(message.sender.id, message.sender.profilePicture)
          );
        },
        error: () => this.notificationService.showError('Failed to load messages'),
      });
  }

  private upsertChat(nextChat: Chat): void {
    this.chats.update(chats => {
      const otherChats = chats.filter(chat => chat.chatId !== nextChat.chatId);
      return [nextChat, ...otherChats];
    });

    nextChat.participants.forEach(participant =>
      this.loadAvatar(participant.id, participant.profilePicture)
    );
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

import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';

import { Chat } from './chat.models';
import { ChatService } from './chat.service';

@Injectable({ providedIn: 'root' })
export class ChatUnreadService {
  private readonly chatService = inject(ChatService);

  readonly totalUnread = signal(0);
  private readonly unreadByChatId = new Map<string, number>();

  syncFromChats(chats: Chat[]): void {
    this.unreadByChatId.clear();
    chats.forEach(chat => this.unreadByChatId.set(chat.chatId, chat.unreadCount ?? 0));
    this.recalculateTotal();
  }

  applyChatUpdate(chat: Chat): void {
    this.unreadByChatId.set(chat.chatId, chat.unreadCount ?? 0);
    this.recalculateTotal();
  }

  removeChat(chatId: string): void {
    this.unreadByChatId.delete(chatId);
    this.recalculateTotal();
  }

  private recalculateTotal(): void {
    let total = 0;
    for (const count of this.unreadByChatId.values()) {
      total += count;
    }
    this.totalUnread.set(total);
  }

  clear(): void {
    this.unreadByChatId.clear();
    this.totalUnread.set(0);
  }

  refresh() {
    return this.chatService.getChats().pipe(
      tap(chats => this.syncFromChats(chats))
    );
  }
}

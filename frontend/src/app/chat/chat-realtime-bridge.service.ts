import { Injectable, inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { Chat } from './chat.models';
import { ChatRealtimeHandlers, ChatRealtimePayload, ChatRealtimeService } from './chat-realtime.service';
import { ChatUnreadService } from './chat-unread.service';

@Injectable({ providedIn: 'root' })
export class ChatRealtimeBridgeService {
  private readonly authService = inject(AuthService);
  private readonly chatRealtimeService = inject(ChatRealtimeService);
  private readonly chatUnreadService = inject(ChatUnreadService);

  private messageHandler: ((payload: ChatRealtimePayload) => void) | null = null;
  private chatListHandler: ((chat: Chat) => void) | null = null;

  ensureConnected(): void {
    const token = this.authService.token();
    if (!token || !this.authService.validateSession() || this.chatRealtimeService.isConnected()) {
      return;
    }

    void this.chatRealtimeService.connect(token, this.createHandlers());
  }

  disconnect(): void {
    this.messageHandler = null;
    this.chatListHandler = null;
    this.chatRealtimeService.disconnect();
  }

  setMessageHandler(handler: ((payload: ChatRealtimePayload) => void) | null): void {
    this.messageHandler = handler;
  }

  setChatListHandler(handler: ((chat: Chat) => void) | null): void {
    this.chatListHandler = handler;
  }

  watchChat(chatId: string | null): void {
    this.chatRealtimeService.watchChat(chatId);
  }

  unwatchChat(): void {
    this.chatRealtimeService.unwatchChat();
  }

  private createHandlers(): ChatRealtimeHandlers {
    return {
      onMessageEvent: payload => this.messageHandler?.(payload),
      onChatSummary: chat => {
        this.chatUnreadService.applyChatUpdate(chat);
        this.chatListHandler?.(chat);
      },
    };
  }
}

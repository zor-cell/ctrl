import { Injectable, inject, untracked } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { Chat } from './chat.models';
import {
  ChatRealtimeHandlers,
  ChatRealtimePayload,
  ChatRealtimeService,
  ConnectionStatus,
} from './chat-realtime.service';
import { ChatUnreadService } from './chat-unread.service';

@Injectable({ providedIn: 'root' })
export class ChatRealtimeBridgeService {
  private readonly authService = inject(AuthService);
  private readonly chatRealtimeService = inject(ChatRealtimeService);
  private readonly chatUnreadService = inject(ChatUnreadService);

  private messageHandler: ((payload: ChatRealtimePayload) => void) | null = null;
  private chatListHandler: ((chat: Chat) => void) | null = null;

  ensureConnected(): void {
    const token = untracked(() => this.authService.token());
    if (
      !token
      || !this.authService.validateSession()
      || this.chatRealtimeService.isConnectionActive()
    ) {
      return;
    }

    void this.chatRealtimeService.connect(token, this.createHandlers());
  }

  /**
   * Reconnect with latest token - useful if token was refreshed
   */
  reconnectWithLatestToken(): void {
    const token = untracked(() => this.authService.token());
    if (!token || !this.authService.validateSession()) {
      console.warn('[ChatRealtimeBridge] Cannot reconnect: no valid token');
      return;
    }

    console.debug('[ChatRealtimeBridge] Reconnecting with latest token');
    void this.chatRealtimeService.connect(token, this.createHandlers());
  }

  whenConnected(): Promise<void> {
    return this.chatRealtimeService.whenConnected();
  }

  /**
   * Get observable for connection status changes
   * Statuses: DISCONNECTED, CONNECTING, CONNECTED, FALLBACK, ERROR
   */
  getConnectionStatus$(): Observable<ConnectionStatus> {
    return this.chatRealtimeService.getConnectionStatus$();
  }

  /**
   * Get current connection status
   */
  getCurrentConnectionStatus(): ConnectionStatus {
    return this.chatRealtimeService.getCurrentConnectionStatus();
  }

  /**
   * Check if currently using WebSocket (true) or fallback HTTP Polling (false)
   */
  isUsingWebSocket(): boolean {
    return this.chatRealtimeService.isUsingWebSocket();
  }

  /**
   * Get last connection error, if any
   */
  getLastConnectionError(): Error | null {
    return this.chatRealtimeService.getLastConnectionError();
  }

  resyncWatchedChat(chatId: string | null): void {
    this.chatRealtimeService.watchChat(chatId);
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

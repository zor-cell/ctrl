import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

import { environment } from '../../environments/environment';
import { Chat, ChatMessage } from './chat.models';

export interface ChatRealtimePayload {
  type: 'MESSAGE' | 'MESSAGE_UPDATE';
  chatId: string;
  message: ChatMessage;
}

export interface ChatRealtimeHandlers {
  onMessageEvent: (payload: ChatRealtimePayload) => void;
  onChatSummary: (chat: Chat) => void;
}

type SockJsFactory = new (url: string, _: unknown, options?: Record<string, unknown>) => WebSocket;

@Injectable({
  providedIn: 'root',
})
export class ChatRealtimeService {
  private client: Client | null = null;
  private chatTopicSubscription: StompSubscription | null = null;
  private chatQueueSubscription: StompSubscription | null = null;
  private handlers: ChatRealtimeHandlers | null = null;
  private activeChatId: string | null = null;
  private sockJsFactory: SockJsFactory | null = null;
  private readonly connectionReadyResolvers: Array<() => void> = [];

  isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  whenConnected(): Promise<void> {
    if (this.client?.connected) {
      return Promise.resolve();
    }

    return new Promise(resolve => this.connectionReadyResolvers.push(resolve));
  }

  async connect(token: string, handlers: ChatRealtimeHandlers): Promise<void> {
    this.handlers = handlers;
    this.deactivateClient();

    try {
      const SockJS = await this.loadSockJsFactory();
      this.client = new Client({
        webSocketFactory: () =>
          new SockJS(this.resolveWsUrl(), null, { transports: ['xhr-streaming', 'xhr-polling'] }),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        onConnect: () => {
          this.chatQueueSubscription = this.client?.subscribe(
            '/user/queue/chats',
            message => this.handleChatSummary(message),
          ) ?? null;
          this.subscribeToActiveChat();
          this.resolveConnectionWaiters();
        },
        onStompError: () => {
          // Keep the app usable when the socket is unavailable.
        },
        onWebSocketError: () => {
          // Keep the app usable when the socket is unavailable.
        },
      });

      this.client.activate();
    } catch {
      // Realtime is optional; HTTP loading still works.
    }
  }

  watchChat(chatId: string | null): void {
    this.activeChatId = chatId;
    this.subscribeToActiveChat();
  }

  unwatchChat(): void {
    this.watchChat(null);
  }

  disconnect(): void {
    this.activeChatId = null;
    this.handlers = null;
    this.clearConnectionWaiters();
    this.deactivateClient();
  }

  private deactivateClient(): void {
    this.chatTopicSubscription?.unsubscribe();
    this.chatTopicSubscription = null;
    this.chatQueueSubscription?.unsubscribe();
    this.chatQueueSubscription = null;

    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }

  private resolveConnectionWaiters(): void {
    while (this.connectionReadyResolvers.length > 0) {
      this.connectionReadyResolvers.shift()?.();
    }
  }

  private clearConnectionWaiters(): void {
    this.connectionReadyResolvers.length = 0;
  }

  private async loadSockJsFactory(): Promise<SockJsFactory> {
    if (this.sockJsFactory) {
      return this.sockJsFactory;
    }

    const module = await import('sockjs-client');
    this.sockJsFactory = (module.default ?? module) as SockJsFactory;
    return this.sockJsFactory;
  }

  private subscribeToActiveChat(): void {
    this.chatTopicSubscription?.unsubscribe();
    this.chatTopicSubscription = null;

    if (!this.client?.connected || !this.activeChatId) {
      return;
    }

    this.chatTopicSubscription = this.client.subscribe(
      `/topic/chats/${this.activeChatId}`,
      message => this.handleMessageEvent(message),
    );
  }

  private resolveWsUrl(): string {
    if ('wsUrl' in environment && typeof environment.wsUrl === 'string') {
      return environment.wsUrl;
    }

    if (environment.production) {
      const origin = window.location.origin.replace(/\/$/, '');
      return `${origin}/ws`;
    }

    return '/ws';
  }

  private handleMessageEvent(message: IMessage): void {
    if (!this.handlers) {
      return;
    }

    const payload = JSON.parse(message.body) as ChatRealtimePayload;
    payload.message = {
      ...payload.message,
      reactions: payload.message.reactions ?? [],
    };
    this.handlers.onMessageEvent(payload);
  }

  private handleChatSummary(message: IMessage): void {
    if (!this.handlers) {
      return;
    }

    const chat = JSON.parse(message.body) as Chat;
    this.handlers.onChatSummary(chat);
  }
}

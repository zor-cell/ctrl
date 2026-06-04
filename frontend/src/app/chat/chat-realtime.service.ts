import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IFrame, IMessage, StompSubscription } from '@stomp/stompjs';

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
  onChatRemoved: (chatId: string) => void;
}

export type ConnectionStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'FALLBACK' | 'ERROR';
type SockJsFactory = new (url: string, protocols?: unknown, options?: Record<string, unknown>) => WebSocket;
const NATIVE_CONNECT_TIMEOUT_MS = 3000;

@Injectable({
  providedIn: 'root',
})
export class ChatRealtimeService {
  private client: Client | null = null;
  private chatTopicSubscription: StompSubscription | null = null;
  private chatQueueSubscription: StompSubscription | null = null;
  private chatRemovedSubscription: StompSubscription | null = null;
  private handlers: ChatRealtimeHandlers | null = null;
  private activeChatId: string | null = null;
  private sockJsFactory: SockJsFactory | null = null;
  private readonly connectionReadyResolvers: (() => void)[] = [];

  private connectionStatus$ = new BehaviorSubject<ConnectionStatus>('DISCONNECTED');
  private currentToken: string | null = null;
  private reconnectAttempts = 0;
  private lastConnectionError: Error | null = null;
  private isUsingFallback = false;
  private fallbackActivationInProgress = false;
  private nativeConnectTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private connectionGeneration = 0;

  isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  isConnectionActive(): boolean {
    const status = this.connectionStatus$.value;
    return status === 'CONNECTING' || status === 'CONNECTED' || status === 'FALLBACK';
  }

  getConnectionStatus$(): Observable<ConnectionStatus> {
    return this.connectionStatus$.asObservable();
  }

  getCurrentConnectionStatus(): ConnectionStatus {
    return this.connectionStatus$.value;
  }

  isUsingWebSocket(): boolean {
    return !this.isUsingFallback;
  }

  getLastConnectionError(): Error | null {
    return this.lastConnectionError;
  }

  whenConnected(): Promise<void> {
    if (this.client?.connected) {
      return Promise.resolve();
    }

    return new Promise(resolve => this.connectionReadyResolvers.push(resolve));
  }

  async connect(token: string, handlers: ChatRealtimeHandlers): Promise<void> {
    const generation = ++this.connectionGeneration;
    this.handlers = handlers;
    this.currentToken = token;
    this.reconnectAttempts = 0;
    this.isUsingFallback = false;
    this.fallbackActivationInProgress = false;
    this.deactivateClient();
    this.connectionStatus$.next('CONNECTING');

    try {
      this.activateNativeClient(token, generation);
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : 'Unknown error';
      console.error('[WebSocket] Connection failed:', errorMsg);
      this.lastConnectionError = error instanceof Error ? error : new Error(errorMsg);
      this.connectionStatus$.next('ERROR');
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
    this.connectionGeneration += 1;
    this.activeChatId = null;
    this.handlers = null;
    this.currentToken = null;
    this.reconnectAttempts = 0;
    this.isUsingFallback = false;
    this.fallbackActivationInProgress = false;
    this.lastConnectionError = null;
    this.clearConnectionWaiters();
    this.deactivateClient();
    this.connectionStatus$.next('DISCONNECTED');
  }

  private deactivateClient(): void {
    this.clearNativeConnectTimeout();
    this.chatTopicSubscription?.unsubscribe();
    this.chatTopicSubscription = null;
    this.chatQueueSubscription?.unsubscribe();
    this.chatQueueSubscription = null;
    this.chatRemovedSubscription?.unsubscribe();
    this.chatRemovedSubscription = null;

    if (this.client) {
      try {
        this.client.deactivate();
      } catch (error) {
        console.warn('[WebSocket] Error during deactivation:', error);
      }
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

  private clearNativeConnectTimeout(): void {
    if (!this.nativeConnectTimeoutId) {
      return;
    }

    clearTimeout(this.nativeConnectTimeoutId);
    this.nativeConnectTimeoutId = null;
  }

  private isActiveConnection(generation: number, client: Client): boolean {
    return generation === this.connectionGeneration && this.client === client;
  }

  private isCurrentConnectionGeneration(generation: number): boolean {
    return generation === this.connectionGeneration;
  }

  private isSockJsSessionClosedFrame(frame: IFrame): boolean {
    return (frame.headers['message'] ?? '').toLowerCase().includes('session closed');
  }

  private activateNativeClient(token: string, generation: number): void {
    let nativeClient: Client;
    nativeClient = new Client({
      brokerURL: this.resolveNativeWebSocketUrl(),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 0,
      heartbeatIncoming: 5000,
      heartbeatOutgoing: 5000,
      onConnect: () => {
        if (!this.isActiveConnection(generation, nativeClient)) {
          return;
        }

        this.handleConnected(false);
      },
      onDisconnect: () => {
        if (!this.isActiveConnection(generation, nativeClient)) {
          return;
        }

        console.debug('[WebSocket] Disconnected');
        if (!this.fallbackActivationInProgress && !this.isUsingFallback) {
          this.connectionStatus$.next('DISCONNECTED');
        }
      },
      onStompError: frame => {
        if (!this.isActiveConnection(generation, nativeClient)) {
          return;
        }

        const errorMsg = `STOMP Error: ${frame.headers['message'] || 'Unknown error'}`;
        console.error('[WebSocket]', errorMsg, frame);
        this.lastConnectionError = new Error(errorMsg);
        this.connectionStatus$.next('ERROR');
      },
      onWebSocketError: event => {
        if (!this.isActiveConnection(generation, nativeClient)) {
          return;
        }

        const errorMsg = 'Native WebSocket connection failed';
        console.error('[WebSocket]', errorMsg, event);
        this.lastConnectionError = new Error(errorMsg);
        void this.activateSockJsFallback(errorMsg, generation);
      },
      onWebSocketClose: event => {
        if (!this.isActiveConnection(generation, nativeClient)) {
          return;
        }

        if (this.connectionStatus$.value === 'CONNECTING' && !this.isUsingFallback) {
          const reason = event.reason ? `: ${event.reason}` : '';
          void this.activateSockJsFallback(
            `Native WebSocket closed before STOMP connect${reason}`,
            generation,
          );
        }
      },
    });

    this.client = nativeClient;
    nativeClient.activate();
    this.nativeConnectTimeoutId = setTimeout(() => {
      if (
        this.isActiveConnection(generation, nativeClient)
        && this.connectionStatus$.value === 'CONNECTING'
        && !nativeClient.connected
        && !this.isUsingFallback
      ) {
        void this.activateSockJsFallback(
          `Native WebSocket did not finish STOMP connect within ${NATIVE_CONNECT_TIMEOUT_MS}ms`,
          generation,
        );
      }
    }, NATIVE_CONNECT_TIMEOUT_MS);
  }

  private async activateSockJsFallback(reason: string, generation: number): Promise<void> {
    if (
      !this.isCurrentConnectionGeneration(generation)
      || this.fallbackActivationInProgress
      || this.isUsingFallback
    ) {
      return;
    }

    const token = this.currentToken;
    if (!token) {
      this.connectionStatus$.next('ERROR');
      return;
    }

    this.fallbackActivationInProgress = true;
    this.isUsingFallback = true;
    this.connectionStatus$.next('FALLBACK');
    console.warn(`[WebSocket] ${reason}. Falling back to SockJS XHR transport.`);
    this.deactivateClient();

    try {
      const SockJS = await this.loadSockJsFactory();
      if (!this.isCurrentConnectionGeneration(generation)) {
        return;
      }

      let fallbackClient: Client;
      fallbackClient = new Client({
        webSocketFactory: () =>
          new SockJS(this.resolveSockJsUrl(), undefined, {
            transports: ['xhr-streaming', 'xhr-polling'],
          }),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 5000,
        heartbeatOutgoing: 5000,
        onConnect: () => {
          if (!this.isActiveConnection(generation, fallbackClient)) {
            return;
          }

          this.handleConnected(true);
        },
        onDisconnect: () => {
          if (!this.isActiveConnection(generation, fallbackClient)) {
            return;
          }

          console.debug('[WebSocket] SockJS fallback disconnected');
          this.connectionStatus$.next('DISCONNECTED');
        },
        onStompError: frame => {
          if (!this.isActiveConnection(generation, fallbackClient)) {
            return;
          }

          if (this.isSockJsSessionClosedFrame(frame)) {
            console.debug('[WebSocket] SockJS fallback session closed; waiting for reconnect.');
            return;
          }

          const errorMsg = `SockJS fallback STOMP Error: ${frame.headers['message'] || 'Unknown error'}`;
          console.error('[WebSocket]', errorMsg, frame);
          this.lastConnectionError = new Error(errorMsg);
          this.connectionStatus$.next('ERROR');
        },
        onWebSocketError: event => {
          if (!this.isActiveConnection(generation, fallbackClient)) {
            return;
          }

          const errorMsg = 'SockJS fallback connection failed';
          console.error('[WebSocket]', errorMsg, event);
          this.lastConnectionError = new Error(errorMsg);
          this.connectionStatus$.next('ERROR');
        },
      });

      this.client = fallbackClient;
      fallbackClient.activate();
    } catch (error) {
      if (!this.isCurrentConnectionGeneration(generation)) {
        return;
      }

      const errorMsg = error instanceof Error ? error.message : 'Unknown SockJS fallback error';
      console.error('[WebSocket] SockJS fallback setup failed:', errorMsg);
      this.lastConnectionError = error instanceof Error ? error : new Error(errorMsg);
      this.connectionStatus$.next('ERROR');
    } finally {
      if (this.isCurrentConnectionGeneration(generation)) {
        this.fallbackActivationInProgress = false;
      }
    }
  }

  private handleConnected(usingFallback: boolean): void {
    this.clearNativeConnectTimeout();
    console.debug(
      usingFallback
        ? '[WebSocket] Connected via SockJS fallback'
        : '[WebSocket] Connected successfully',
    );
    this.reconnectAttempts = 0;
    this.isUsingFallback = usingFallback;
    this.lastConnectionError = null;

    this.chatQueueSubscription = this.client?.subscribe(
      '/user/queue/chats',
      message => this.handleChatSummary(message),
    ) ?? null;
    this.chatRemovedSubscription = this.client?.subscribe(
      '/user/queue/chats/removed',
      message => this.handleChatRemoved(message),
    ) ?? null;
    this.subscribeToActiveChat();
    this.connectionStatus$.next(usingFallback ? 'FALLBACK' : 'CONNECTED');
    this.resolveConnectionWaiters();
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
      `/user/queue/chats/${this.activeChatId}/messages`,
      message => this.handleMessageEvent(message),
    );
  }

  private resolveNativeWebSocketUrl(): string {
    if ('wsUrl' in environment && typeof environment.wsUrl === 'string') {
      return environment.wsUrl;
    }

    if (environment.production) {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      return `${protocol}//${window.location.host}/ws`;
    }

    return 'ws://localhost:8080/ws';
  }

  private resolveSockJsUrl(): string {
    if (environment.production) {
      return `${window.location.origin.replace(/\/$/, '')}/ws-sockjs`;
    }

    return this.resolveNativeWebSocketUrl()
      .replace(/^ws:/, 'http:')
      .replace(/^wss:/, 'https:')
      .replace(/\/ws$/, '/ws-sockjs');
  }

  private handleMessageEvent(message: IMessage): void {
    if (!this.handlers) {
      return;
    }

    try {
      const payload = JSON.parse(message.body) as ChatRealtimePayload;
      payload.message = {
        ...payload.message,
        reactions: payload.message.reactions ?? [],
      };
      this.handlers.onMessageEvent(payload);
    } catch (error) {
      console.error('[WebSocket] Error handling message event:', error, message.body);
    }
  }

  private handleChatSummary(message: IMessage): void {
    if (!this.handlers) {
      return;
    }

    try {
      const chat = JSON.parse(message.body) as Chat;
      this.handlers.onChatSummary(chat);
    } catch (error) {
      console.error('[WebSocket] Error handling chat summary:', error, message.body);
    }
  }

  private handleChatRemoved(message: IMessage): void {
    if (!this.handlers) {
      return;
    }

    try {
      const payload = JSON.parse(message.body) as { chatId: string };
      this.handlers.onChatRemoved(payload.chatId);
    } catch (error) {
      console.error('[WebSocket] Error handling chat removal:', error, message.body);
    }
  }
}

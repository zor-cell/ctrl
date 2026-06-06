import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { ChatRealtimeHandlers, ChatRealtimeService } from './chat-realtime.service';

const activateMock = vi.fn();
const deactivateMock = vi.fn().mockResolvedValue(undefined);
const subscribeMock = vi.fn().mockReturnValue({ unsubscribe: vi.fn() });
let lastClientConfig: Record<string, unknown> | null = null;

vi.mock('@stomp/stompjs', () => {
  class MockClient {
    connected = false;
    activate = activateMock;
    deactivate = deactivateMock;
    subscribe = subscribeMock;

    constructor(config: Record<string, unknown>) {
      lastClientConfig = config;
    }
  }

  return { Client: MockClient };
});

vi.mock('sockjs-client', () => ({
  default: class MockSockJS {},
}));

const environmentWithFlags = environment as { wsPreferSockJs?: boolean };
const originalPreferSockJs = environmentWithFlags.wsPreferSockJs;

const handlers: ChatRealtimeHandlers = {
  onMessageEvent: vi.fn(),
  onChatSummary: vi.fn(),
  onChatRemoved: vi.fn(),
};

describe('ChatRealtimeService', () => {
  let service: ChatRealtimeService;

  beforeEach(() => {
    activateMock.mockClear();
    deactivateMock.mockClear();
    subscribeMock.mockClear();
    lastClientConfig = null;

    vi.stubGlobal('location', {
      protocol: 'http:',
      host: 'localhost:4200',
      origin: 'http://localhost:4200',
    });

    environmentWithFlags.wsPreferSockJs = false;

    TestBed.configureTestingModule({});
    service = TestBed.inject(ChatRealtimeService);
  });

  afterEach(() => {
    service.disconnect();
    environmentWithFlags.wsPreferSockJs = originalPreferSockJs;
  });

  it('starts native WebSocket STOMP connect with bearer token', async () => {
    await service.connect('test-token', handlers);

    expect(activateMock).toHaveBeenCalledTimes(1);
    expect(lastClientConfig?.['brokerURL']).toBe('ws://127.0.0.1:8080/ws');
    expect(lastClientConfig?.['webSocketFactory']).toEqual(expect.any(Function));
    expect(lastClientConfig?.['connectHeaders']).toEqual({
      Authorization: 'Bearer test-token',
      'heart-beat': '0,0',
    });
    expect(lastClientConfig?.['heartbeatIncoming']).toBe(0);
    expect(lastClientConfig?.['heartbeatOutgoing']).toBe(0);
    expect(service.getCurrentConnectionStatus()).toBe('CONNECTING');
  });

  it('skips the native attempt and uses the SockJS transport when configured', async () => {
    environmentWithFlags.wsPreferSockJs = true;

    await service.connect('test-token', handlers);
    await vi.waitFor(() => expect(activateMock).toHaveBeenCalledTimes(1));

    expect(lastClientConfig?.['brokerURL']).toBeUndefined();
    expect(lastClientConfig?.['webSocketFactory']).toEqual(expect.any(Function));
    expect(service.getCurrentConnectionStatus()).toBe('SOCKJS');
  });

  it('disconnects without touching a client that never activated', () => {
    service.disconnect();

    expect(deactivateMock).not.toHaveBeenCalled();
    expect(service.getCurrentConnectionStatus()).toBe('DISCONNECTED');
  });

  it('deactivates an in-flight client when disconnect is called', async () => {
    await service.connect('test-token', handlers);
    service.disconnect();

    expect(deactivateMock).toHaveBeenCalledWith({ force: true });
    expect(service.getCurrentConnectionStatus()).toBe('DISCONNECTED');
    expect(service.isConnectionActive()).toBe(false);
  });

  it('ignores stale connect callbacks after a newer disconnect generation', async () => {
    await service.connect('first-token', handlers);
    const onConnect = lastClientConfig?.['onConnect'] as (() => void) | undefined;
    expect(onConnect).toBeDefined();

    service.disconnect();
    onConnect?.();

    expect(service.getCurrentConnectionStatus()).toBe('DISCONNECTED');
    expect(subscribeMock).not.toHaveBeenCalled();
  });
});

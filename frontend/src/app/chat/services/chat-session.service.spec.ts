import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ChatRealtimeBridgeService } from './chat-realtime-bridge.service';
import { ChatSessionService } from './chat-session.service';
import { ChatUnreadService } from './chat-unread.service';

describe('ChatSessionService', () => {
  let service: ChatSessionService;
  let bridge: {
    ensureConnected: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    isConnectionActive: ReturnType<typeof vi.fn>;
  };
  let unread: { refresh: ReturnType<typeof vi.fn>; clear: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    bridge = {
      ensureConnected: vi.fn(),
      disconnect: vi.fn(),
      isConnectionActive: vi.fn().mockReturnValue(false),
    };
    unread = {
      refresh: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
      clear: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        ChatSessionService,
        { provide: ChatRealtimeBridgeService, useValue: bridge },
        { provide: ChatUnreadService, useValue: unread },
      ],
    });

    service = TestBed.inject(ChatSessionService);
  });

  it('bootstraps unread refresh and realtime connect', () => {
    service.bootstrap();

    expect(unread.refresh).toHaveBeenCalledTimes(1);
    expect(bridge.ensureConnected).toHaveBeenCalledTimes(1);
  });

  it('clears unread state on login teardown without disconnecting idle sockets', () => {
    service.teardown();

    expect(unread.clear).toHaveBeenCalledTimes(1);
    expect(bridge.disconnect).not.toHaveBeenCalled();
  });

  it('disconnects realtime transport when a session is still active', () => {
    bridge.isConnectionActive.mockReturnValue(true);

    service.teardown();

    expect(unread.clear).toHaveBeenCalledTimes(1);
    expect(bridge.disconnect).toHaveBeenCalledTimes(1);
  });
});

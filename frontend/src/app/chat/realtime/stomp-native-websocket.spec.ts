import { describe, expect, it } from 'vitest';

import { createStompNativeWebSocket } from './stomp-native-websocket';

describe('createStompNativeWebSocket', () => {
  it('ignores binaryType assignment from stompjs', () => {
    const socket = createStompNativeWebSocket('ws://example.test/ws');

    socket.binaryType = 'arraybuffer';

    expect(socket.binaryType).not.toBe('arraybuffer');
  });

  it('returns a real WebSocket instance so brand-checked setters keep working', () => {
    const socket = createStompNativeWebSocket('ws://example.test/ws');

    expect(socket).toBeInstanceOf(WebSocket);
    expect(() => {
      socket.onmessage = () => undefined;
    }).not.toThrow();
  });
});

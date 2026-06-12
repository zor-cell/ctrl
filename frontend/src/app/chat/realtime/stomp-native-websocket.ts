/** STOMP subprotocols supported by Spring's WebSocket message broker. */
export const STOMP_WEBSOCKET_PROTOCOLS = ['v12.stomp', 'v11.stomp', 'v10.stomp'] as const;

/**
 * Creates a WebSocket for STOMP that stays compatible with WebKit/Safari.
 *
 * @stomp/stompjs forces {@link WebSocket.binaryType} = `arraybuffer`, which causes Safari/WebKit
 * to mis-parse server text frames as invalid opcodes (e.g. opCode 7). We keep the real WebSocket
 * instance (so brand checks like the `onmessage` setter still work) but neutralize the
 * `binaryType` assignment so text STOMP frames remain strings.
 */
export function createStompNativeWebSocket(brokerURL: string): WebSocket {
  const socket = new WebSocket(brokerURL, [...STOMP_WEBSOCKET_PROTOCOLS]);

  try {
    Object.defineProperty(socket, 'binaryType', {
      configurable: true,
      enumerable: false,
      get: () => 'blob',
      set: () => undefined,
    });
  } catch {
    // If the property cannot be redefined, fall back to native behavior.
  }

  return socket;
}

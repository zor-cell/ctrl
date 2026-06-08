#!/usr/bin/env node
/**
 * Live check against a running backend on :8080 (e.g. Docker).
 * Note: the Node "ws" client can fail on macOS with opCode 7; prefer verify:websocket (Maven).
 */
import { Client } from '@stomp/stompjs';
import WebSocket from 'ws';

const apiBase = process.env.CTRL_API_BASE ?? 'http://localhost:8080/api';
const wsUrl = process.env.CTRL_WS_URL ?? 'ws://localhost:8080/ws';
const timeoutMs = 10_000;

function fail(message) {
  console.error(`[verify-native-websocket-live] FAIL: ${message}`);
  process.exit(1);
}

async function registerAndLogin() {
  const suffix = Date.now().toString(36);
  const username = `ws_live_${suffix}`;
  const password = 'Password123!';

  const registerResponse = await fetch(`${apiBase}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username,
      password,
      email: `${username}@example.test`,
      name: 'WS Live Verify',
      birthdate: '2000-01-01',
    }),
  });

  if (!registerResponse.ok) {
    fail(`register failed with status ${registerResponse.status}`);
  }

  const loginResponse = await fetch(`${apiBase}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });

  if (!loginResponse.ok) {
    fail(`login failed with status ${loginResponse.status}`);
  }

  const body = await loginResponse.json();
  if (!body?.token) {
    fail('login response did not include a token');
  }

  return body.token;
}

function connectNative(token) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      client.deactivate();
      reject(new Error(`Timed out after ${timeoutMs}ms waiting for STOMP connect`));
    }, timeoutMs);

    const client = new Client({
      brokerURL: wsUrl,
      webSocketFactory: () => {
        const socket = new WebSocket(wsUrl, ['v12.stomp', 'v11.stomp', 'v10.stomp'], {
          perMessageDeflate: false,
        });
        socket.on('upgrade', res => {
          const ext = res.headers['sec-websocket-extensions'];
          console.log(`[verify] negotiated extensions: ${ext ?? '(none)'}`);
        });
        return socket;
      },
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 0,
      heartbeatIncoming: 0,
      heartbeatOutgoing: 0,
      debug: () => undefined,
      onConnect: () => {
        clearTimeout(timeout);
        client.deactivate().then(() => resolve());
      },
      onStompError: frame => {
        clearTimeout(timeout);
        reject(new Error(frame.headers['message'] ?? 'STOMP error'));
      },
      onWebSocketError: () => {
        clearTimeout(timeout);
        reject(new Error('Native WebSocket connection failed'));
      },
      onWebSocketClose: event => {
        if (!client.connected) {
          clearTimeout(timeout);
          reject(
            new Error(
              `WebSocket closed before STOMP connect (code=${event.code}, reason=${event.reason || 'n/a'})`,
            ),
          );
        }
      },
    });

    client.activate();
  });
}

try {
  const token = await registerAndLogin();
  await connectNative(token);
  console.log(`[verify-native-websocket-live] OK: connected to ${wsUrl}`);
} catch (error) {
  fail(error instanceof Error ? error.message : String(error));
}

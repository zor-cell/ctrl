#!/usr/bin/env node
/**
 * Verifies native STOMP/WebSocket against a running backend (Docker on :8080).
 * Uses the Java WebSocket client via Maven integration test (reliable on macOS).
 */
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const backendDir = path.resolve(scriptDir, '../../backend');

const result = spawnSync(
  'mvn',
  [
    '-q',
    'test',
    '-Dtest=WebSocketSecurityIntegrationTest#authenticatedNativeWebSocketConnection_receivesChatMessageEventsAndUserQueueUpdates',
    '-Djacoco.skip=true',
  ],
  {
    cwd: backendDir,
    stdio: 'inherit',
    env: process.env,
  },
);

if (result.status === 0) {
  console.log('[verify-native-websocket] OK: native STOMP/WebSocket integration test passed');
  process.exit(0);
}

console.error('[verify-native-websocket] FAIL: integration test failed');
process.exit(result.status ?? 1);

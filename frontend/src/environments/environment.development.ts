export const environment = {
  production: false,
  apiUrl: '/api',
  // Direct backend WebSocket — Vite dev proxy can corrupt STOMP frames (opCode 7).
  wsUrl: 'ws://127.0.0.1:8080/ws',
  // Native-first: every machine tries the native WebSocket and only falls back to SockJS at
  // runtime if it actually fails. Override this to true ONLY locally (uncommitted) when VPN/
  // socket-filter software (e.g. Cisco Secure Client, GlobalProtect) corrupts native WebSocket
  // frames on macOS (server reports "opCode 7") and you want to skip the failing native attempt.
  wsPreferSockJs: false,
  idAustria: {
    clientId: 'http://localhost:8080',
    redirectUri: 'http://localhost:4200/verification-callback',
    authUrl: 'http://localhost:8081/realms/id-austria-mock/protocol/openid-connect/auth',
    scope: 'openid profile'
  },
  auth: {
    requireEmailVerification: false
  },
  heartbeat: {
    interval: 10000 // 10 s in ms
  }
};

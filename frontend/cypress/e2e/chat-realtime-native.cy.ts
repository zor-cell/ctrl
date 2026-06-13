import { Client } from '@stomp/stompjs';

describe('Chat realtime native WebSocket', () => {
  const apiBase = Cypress.env('apiUrl') ?? 'http://localhost:8080';
  const apiUrl = `${apiBase}/api`;
  const wsUrl = Cypress.env('wsUrl') ?? 'ws://localhost:8080/ws';

  before(function () {
    if (!Cypress.env('backendE2E')) {
      this.skip();
    }
  });

  it('connects to STOMP over native WebSocket', () => {
    const suffix = Date.now().toString(36);
    const username = `native_${suffix}`;
    const password = 'Password123!';

    cy.request({
      method: 'POST',
      url: `${apiUrl}/auth/register`,
      body: {
        username,
        password,
        email: `${username}@example.test`,
        name: 'Native WebSocket',
        birthdate: '2000-01-01',
      },
    });

    cy.request<{ token: string }>({
      method: 'POST',
      url: `${apiUrl}/auth/login`,
      body: {
        username,
        password,
      },
    }).then(response => {
      const token = response.body.token;

      return new Cypress.Promise<void>((resolve, reject) => {
        const timeout = window.setTimeout(() => {
          client.deactivate();
          reject(new Error('Timed out while waiting for native WebSocket STOMP connect'));
        }, 8000);

        const client = new Client({
          brokerURL: wsUrl,
          connectHeaders: {
            Authorization: `Bearer ${token}`,
          },
          reconnectDelay: 0,
          heartbeatIncoming: 0,
          heartbeatOutgoing: 0,
          debug: () => undefined,
          onConnect: () => {
            window.clearTimeout(timeout);
            expect(client.connected).to.equal(true);
            client.deactivate().then(() => resolve());
          },
          onStompError: frame => {
            window.clearTimeout(timeout);
            reject(new Error(frame.headers['message'] ?? 'STOMP error'));
          },
          onWebSocketError: () => {
            window.clearTimeout(timeout);
            reject(new Error('Native WebSocket connection failed'));
          },
        });

        client.activate();
      });
    });
  });
});

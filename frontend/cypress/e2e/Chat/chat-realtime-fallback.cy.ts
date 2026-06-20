import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

describe('Chat realtime SockJS fallback', () => {
  const apiBase = Cypress.env('apiUrl') ?? 'http://localhost:8080';
  const apiUrl = `${apiBase}/api`;

  before(function () {
    if (!Cypress.env('backendE2E')) {
      this.skip();
    }
  });

  it('connects to STOMP over forced SockJS XHR polling', () => {
    const suffix = Date.now().toString(36);
    const username = `polling_${suffix}`;
    const password = 'Password123!';

    cy.request({
      method: 'POST',
      url: `${apiUrl}/auth/register`,
      body: {
        username,
        password,
        email: `${username}@example.test`,
        name: 'Polling Fallback',
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
          reject(new Error('Timed out while waiting for SockJS XHR polling STOMP connect'));
        }, 8000);

        const client = new Client({
          connectHeaders: {
            Authorization: `Bearer ${token}`,
          },
          webSocketFactory: () =>
            new SockJS(`${apiBase}/ws-sockjs`, undefined, {
              transports: ['xhr-polling'],
            }) as WebSocket,
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
          onWebSocketError: event => {
            window.clearTimeout(timeout);
            reject(new Error(`SockJS XHR polling failed: ${JSON.stringify(event)}`));
          },
        });

        client.activate();
      });
    });
  });
});

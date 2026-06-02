describe('Chat live messaging', () => {
  const apiBase = Cypress.env('apiUrl') ?? 'http://localhost:8080';
  const apiUrl = `${apiBase}/api`;

  before(function () {
    if (!Cypress.env('backendE2E')) {
      this.skip();
    }
  });

  it('renders a message received through realtime while the chat is open', () => {
    let aliceToken: string;
    let bobToken: string;
    let chatId: string;

    cy.request<{ token: string }>({
      method: 'POST',
      url: `${apiUrl}/auth/login`,
      body: {
        username: 'test',
        password: 'test',
      },
    })
      .then(response => {
        aliceToken = response.body.token;

        return cy.request<{ token: string }>({
          method: 'POST',
          url: `${apiUrl}/auth/login`,
          body: {
            username: 'test1',
            password: 'test1',
          },
        });
      })
      .then(response => {
        bobToken = response.body.token;

        return cy.request<{ chatId: string }>({
          method: 'POST',
          url: `${apiUrl}/chats`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          body: {
            usernames: ['test1'],
          },
        });
      })
      .then(response => {
        chatId = response.body.chatId;

        cy.visit('/chat', {
          onBeforeLoad(win) {
            win.localStorage.setItem('AUTH_TOKEN', bobToken);
          },
        });

        cy.contains('button.chat-row', 'test', { timeout: 15000 }).click();
        cy.get('.chat-header').should('contain.text', 'test');

        const message = `live e2e ${Date.now()}`;

        cy.request({
          method: 'POST',
          url: `${apiUrl}/chats/${chatId}/messages`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          body: {
            content: message,
            postIdToShare: null,
          },
        });

        cy.contains('.message-content', message, { timeout: 10000 }).should('be.visible');
      });
  });
});

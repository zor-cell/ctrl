describe('Chat message pagination', () => {
  const apiBase = Cypress.env('apiUrl') ?? 'http://localhost:8080';
  const apiUrl = `${apiBase}/api`;
  const pageSize = 30;

  before(function () {
    if (!Cypress.env('backendE2E')) {
      this.skip();
    }
  });

  it('loads the latest messages first and fetches older messages when scrolling up', () => {
    const prefix = `pagination e2e ${Date.now()}`;
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

        for (let index = 1; index <= 36; index += 1) {
          cy.request({
            method: 'POST',
            url: `${apiUrl}/chats/${chatId}/messages`,
            headers: {
              Authorization: `Bearer ${aliceToken}`,
            },
            body: {
              content: `${prefix} message ${index.toString().padStart(2, '0')}`,
              postIdToShare: null,
            },
          });
        }

        cy.visit('/chat', {
          onBeforeLoad(win) {
            win.localStorage.setItem('AUTH_TOKEN', bobToken);
          },
        });

        cy.contains('button.chat-row', 'test', { timeout: 15000 }).click();
        cy.get('.chat-header').should('contain.text', 'test');
        cy.contains('.message-content', `${prefix} message 36`, { timeout: 10000 })
          .should('be.visible');

        cy.get('.message-content').should(contents => {
          const matchingMessages = contents
            .toArray()
            .filter(content => content.textContent?.trim().startsWith(prefix));

          expect(matchingMessages).to.have.length(pageSize);
          expect(
            matchingMessages.some(
              content => content.textContent?.trim() === `${prefix} message 01`,
            ),
          ).to.equal(false);
        });

        cy.get('.message-list').scrollTo('top');

        cy.get('.message-content', { timeout: 10000 }).should(contents => {
          const matchingMessages = contents
            .toArray()
            .filter(content => content.textContent?.trim().startsWith(prefix));

          expect(matchingMessages.length).to.be.greaterThan(pageSize);
          expect(
            matchingMessages.some(
              content => content.textContent?.trim() === `${prefix} message 01`,
            ),
          ).to.equal(true);
        });
      });
  });
});

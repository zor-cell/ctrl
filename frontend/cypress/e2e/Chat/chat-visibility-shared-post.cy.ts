describe('Chat visibility and shared posts', () => {
  const apiBase = Cypress.env('apiUrl') ?? 'http://localhost:8080';
  const apiUrl = `${apiBase}/api`;

  before(function () {
    if (!Cypress.env('backendE2E')) {
      this.skip();
    }
  });

  it('shows removed shared posts as unavailable and hides blocked direct chats until unblocked', () => {
    const caption = `shared post e2e ${Date.now()}`;
    let aliceToken: string;
    let bobToken: string;
    let chatId: string;
    let postId: string;

    cy.request<{ token: string }>({
      method: 'POST',
      url: `${apiUrl}/auth/login`,
      body: {
        username: 'test',
        password: 'test',
      },
    })
      .then((response) => {
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
      .then((response) => {
        bobToken = response.body.token;

        cy.request({
          method: 'DELETE',
          url: `${apiUrl}/users/me/block/test1`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          failOnStatusCode: false,
        });

        cy.request({
          method: 'DELETE',
          url: `${apiUrl}/users/me/block/test`,
          headers: {
            Authorization: `Bearer ${bobToken}`,
          },
          failOnStatusCode: false,
        });

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
      .then((response) => {
        chatId = response.body.chatId;

        return cy.request<{ postId: string }>({
          method: 'POST',
          url: `${apiUrl}/posts`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          body: {
            caption,
            mediaRef: null,
            isPublic: true,
            isAiGenerated: false,
            topicIds: [],
            source: null,
          },
        });
      })
      .then((response) => {
        postId = response.body.postId;

        return cy.request({
          method: 'POST',
          url: `${apiUrl}/chats/${chatId}/messages`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          body: {
            content: '',
            postIdToShare: postId,
          },
        });
      })
      .then(() =>
        cy.request({
          method: 'DELETE',
          url: `${apiUrl}/posts/${postId}`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
        }),
      )
      .then(() => {
        cy.visit('/chat', {
          onBeforeLoad(win) {
            win.localStorage.setItem('AUTH_TOKEN', bobToken);
          },
        });

        cy.contains('button.chat-row', 'test', { timeout: 15000 }).click();
        cy.contains('.shared-post-unavailable', 'Post unavailable', { timeout: 10000 }).should(
          'be.visible',
        );
        cy.contains(
          '.shared-post-unavailable',
          'This post is no longer available and can no longer be viewed.',
        ).should('be.visible');

        cy.request({
          method: 'POST',
          url: `${apiUrl}/users/me/block/test`,
          headers: {
            Authorization: `Bearer ${bobToken}`,
          },
        });

        cy.reload();
        cy.contains('button.chat-row', 'test', { timeout: 10000 }).should('not.exist');

        cy.request({
          method: 'DELETE',
          url: `${apiUrl}/users/me/block/test`,
          headers: {
            Authorization: `Bearer ${bobToken}`,
          },
        });

        cy.reload();
        cy.contains('button.chat-row', 'test', { timeout: 10000 }).should('be.visible');
      });
  });

  it('shows a private shared post as unavailable after friendship is removed', () => {
    const caption = `private shared post e2e ${Date.now()}`;
    let aliceToken: string;
    let bobToken: string;
    let chatId: string;
    let postId: string;

    cy.request<{ token: string }>({
      method: 'POST',
      url: `${apiUrl}/auth/login`,
      body: {
        username: 'test',
        password: 'test',
      },
    })
      .then((response) => {
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
      .then((response) => {
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
      .then((response) => {
        chatId = response.body.chatId;

        return cy.request<{ postId: string }>({
          method: 'POST',
          url: `${apiUrl}/posts`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          body: {
            caption,
            mediaRef: null,
            isPublic: false,
            isAiGenerated: false,
            topicIds: [],
            source: null,
          },
        });
      })
      .then((response) => {
        postId = response.body.postId;

        return cy.request({
          method: 'POST',
          url: `${apiUrl}/chats/${chatId}/messages`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          body: {
            content: '',
            postIdToShare: postId,
          },
        });
      })
      .then(() =>
        cy.request({
          method: 'DELETE',
          url: `${apiUrl}/users/me/friends/test`,
          headers: {
            Authorization: `Bearer ${bobToken}`,
          },
        }),
      )
      .then(() => {
        cy.visit('/chat', {
          onBeforeLoad(win) {
            win.localStorage.setItem('AUTH_TOKEN', bobToken);
          },
        });

        cy.contains('button.chat-row', 'test', { timeout: 15000 }).click();
        cy.contains('.shared-post-unavailable', 'Private post from test', {
          timeout: 10000,
        }).should('be.visible');
        cy.contains(
          '.shared-post-unavailable',
          'You cannot view this private post because you are not friends with this account.',
        ).should('be.visible');
      })
      .then(() =>
        cy.request({
          method: 'POST',
          url: `${apiUrl}/users/me/friend-requests`,
          headers: {
            Authorization: `Bearer ${bobToken}`,
          },
          body: {
            toUsername: 'test',
          },
        }),
      )
      .then(() =>
        cy.request({
          method: 'PATCH',
          url: `${apiUrl}/users/me/friend-requests/test1`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          body: {
            action: 'ACCEPT',
          },
        }),
      );
  });

  it('hides a group chat from a removed member and rejects new messages', () => {
    const groupName = `group_e2e_${Date.now()}`;
    let aliceToken: string;
    let bobToken: string;
    let chatId: string;
    let bobId: string;

    cy.request<{ token: string }>({
      method: 'POST',
      url: `${apiUrl}/auth/login`,
      body: {
        username: 'test',
        password: 'test',
      },
    })
      .then((response) => {
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
      .then((response) => {
        bobToken = response.body.token;

        return cy.request<{
          chatId: string;
          participants: Array<{ id: string; username: string }>;
        }>({
          method: 'POST',
          url: `${apiUrl}/chats`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
          body: {
            name: groupName,
            usernames: ['test1'],
          },
        });
      })
      .then((response) => {
        chatId = response.body.chatId;
        bobId = response.body.participants.find(
          (participant) => participant.username === 'test1',
        )!.id;

        return cy.request({
          method: 'DELETE',
          url: `${apiUrl}/chats/${chatId}/members/${bobId}`,
          headers: {
            Authorization: `Bearer ${aliceToken}`,
          },
        });
      })
      .then(() =>
        cy.request({
          method: 'POST',
          url: `${apiUrl}/chats/${chatId}/messages`,
          headers: {
            Authorization: `Bearer ${bobToken}`,
          },
          failOnStatusCode: false,
          body: {
            content: 'should not be accepted',
            postIdToShare: null,
          },
        }),
      )
      .then((response) => {
        expect(response.status).to.eq(403);

        cy.visit('/chat', {
          onBeforeLoad(win) {
            win.localStorage.setItem('AUTH_TOKEN', bobToken);
          },
        });

        cy.contains('button.chat-row', groupName, { timeout: 10000 }).should('not.exist');
      });
  });
});

interface ChatParticipant {
  id: string;
  username: string;
}

import { environment } from '../../../src/environments/environment';

const describeChat = environment.auth.requireEmailVerification ? describe.skip : describe;


interface Chat {
  chatId: string;
  name: string;
  groupChat: boolean;
  participants: ChatParticipant[];
}

interface ChatMessage {
  messageId: string;
  content: string;
  sharedPost: {
    postId: string;
    caption: string;
  } | null;
}

interface Post {
  postId: string;
  caption: string;
}

describeChat('Chat critical flows', () => {
  const apiBase = Cypress.env('apiUrl') ?? 'http://localhost:8080';
  const apiUrl = `${apiBase}/api`;
  const password = '$TestUser123';
  let cleanupUsers: string[] = [];

  beforeEach(() => {
    cleanupUsers = [];
  });

  afterEach(() => {
    cy.wrap([...cleanupUsers].reverse()).each((username) => {
      return deleteUser(username as string);
    });
  });

  function registerUser(username: string, name: string): Cypress.Chainable<Cypress.Response<unknown>> {
    return cy
      .request({
        method: 'POST',
        url: `${apiUrl}/auth/register`,
        body: {
          username,
          password,
          email: `${username}@example.com`,
          name,
          birthdate: '2000-01-01',
        },
      })
      .then((response) => {
        cleanupUsers.push(username);

        return response;
      });
  }

  function loginApiWithPassword(username: string, userPassword: string): Cypress.Chainable<string> {
    return cy
      .request({
        method: 'POST',
        url: `${apiUrl}/auth/login`,
        body: {
          username,
          password: userPassword,
        },
      })
      .then((response) => {
        const body = typeof response.body === 'string' ? JSON.parse(response.body) : response.body;

        expect(response.status, `login status for ${username}`).to.eq(200);
        expect(body, `login body for ${username}`).to.have.property('token');

        return body.token as string;
      });
  }

  function loginApi(username: string): Cypress.Chainable<string> {
    return loginApiWithPassword(username, password);
  }

  function loginSeedUser(): Cypress.Chainable<string> {
    return loginApiWithPassword('test', 'test');
  }

  function deleteUser(username: string): Cypress.Chainable<unknown> {
    return cy
      .request<{ token: string }>({
        method: 'POST',
        url: `${apiUrl}/auth/login`,
        body: {
          username,
          password,
        },
        failOnStatusCode: false,
      })
      .then((loginResponse) => {
        const token = loginResponse.body?.token;

        if (!token) {
          return;
        }

        return cy.request({
          method: 'DELETE',
          url: `${apiUrl}/users/me`,
          headers: {
            Authorization: `Bearer ${token}`,
          },
          failOnStatusCode: false,
        });
      });
  }

  function createFriendship(ownerUsername: string, friendUsername: string): Cypress.Chainable<unknown> {
    let ownerToken: string;

    return loginApi(ownerUsername)
      .then((token) => {
        ownerToken = token;

        return cy.request({
          method: 'POST',
          url: `${apiUrl}/users/me/friend-requests`,
          headers: {
            Authorization: `Bearer ${ownerToken}`,
          },
          body: {
            toUsername: friendUsername,
          },
        });
      })
      .then(() => loginApi(friendUsername))
      .then((friendToken) =>
        cy.request({
          method: 'PATCH',
          url: `${apiUrl}/users/me/friend-requests/${ownerUsername}`,
          headers: {
            Authorization: `Bearer ${friendToken}`,
          },
          body: {
            action: 'ACCEPT',
          },
        })
      );
  }

  function createChat(
    token: string,
    usernames: string[],
    name?: string
  ): Cypress.Chainable<Cypress.Response<Chat>> {
    return cy.request<Chat>({
      method: 'POST',
      url: `${apiUrl}/chats`,
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: {
        name,
        usernames,
      },
    });
  }

  function deleteChat(token: string, chatId: string): Cypress.Chainable<Cypress.Response<unknown>> {
    return cy.request({
      method: 'DELETE',
      url: `${apiUrl}/chats/${chatId}`,
      headers: {
        Authorization: `Bearer ${token}`,
      },
      failOnStatusCode: false,
    });
  }

  function getChats(token: string): Cypress.Chainable<Cypress.Response<Chat[]>> {
    return cy.request<Chat[]>({
      method: 'GET',
      url: `${apiUrl}/chats`,
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  function createPost(token: string, caption: string): Cypress.Chainable<Cypress.Response<Post>> {
    return cy.request<Post>({
      method: 'POST',
      url: `${apiUrl}/posts`,
      headers: {
        Authorization: `Bearer ${token}`,
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
  }

  function deletePost(token: string, postId: string): Cypress.Chainable<Cypress.Response<unknown>> {
    return cy.request({
      method: 'DELETE',
      url: `${apiUrl}/posts/${postId}`,
      headers: {
        Authorization: `Bearer ${token}`,
      },
      failOnStatusCode: false,
    });
  }

  function visitChatWithToken(token: string): void {
    cy.visit('/chat', {
      onBeforeLoad(win) {
        win.localStorage.setItem('AUTH_TOKEN', token);
      },
    });
  }

  function hasParticipant(chat: Chat, username: string): boolean {
    return chat.participants.some((participant) => participant.username === username);
  }

  it('creates a direct chat with one friend', () => {
    const friendUsername = 'Chris';
    const friendName = 'Chris';
    let authToken: string;

    loginSeedUser()
      .then((token) => {
        authToken = token;

        return getChats(authToken);
      })
      .then((chatsResponse) => {
        const existingDirectChats = chatsResponse.body.filter(
          (chat) => !chat.groupChat && hasParticipant(chat, friendUsername)
        );

        return cy.wrap(existingDirectChats).each((chat) => {
          return deleteChat(authToken, (chat as Chat).chatId);
        });
      })
      .then(() => {
        visitChatWithToken(authToken);

        cy.intercept('POST', '**/api/chats').as('createDirectChat');

        cy.get('input[placeholder="Search friends or chats"]', { timeout: 15000 })
          .should('be.visible')
          .type(friendUsername);
        cy.contains('button.friend-row', friendUsername, { timeout: 15000 })
          .should('contain.text', 'New chat')
          .click();

        cy.wait('@createDirectChat').then((interception) => {
          expect(interception.response?.statusCode).to.be.oneOf([200, 201]);
          const createdChat = interception.response?.body as Chat;

          expect(createdChat.groupChat).to.eq(false);
          expect(hasParticipant(createdChat, friendUsername)).to.eq(true);

          cy.get('.chat-header', { timeout: 15000 }).should('contain.text', friendName);
          cy.contains('button.chat-row', friendName).should('be.visible');

          deleteChat(authToken, createdChat.chatId);
        });
      });
  });

  it('creates a group chat with friends', () => {
    const groupName = `e2e_group_${Date.now()}`;

    loginSeedUser()
      .then((token) => createChat(token, ['Felix', 'Franz'], groupName))
      .then((response) => {
        const chat = response.body;

        expect(response.status).to.eq(201);
        expect(chat.groupChat).to.eq(true);
        expect(chat.name).to.eq(groupName);
        expect(hasParticipant(chat, 'Felix')).to.eq(true);
        expect(hasParticipant(chat, 'Franz')).to.eq(true);

        return loginSeedUser().then((token) => deleteChat(token, chat.chatId));
      });
  });

  it('shares a post into a direct chat', () => {
    const caption = `E2E shared post ${Date.now()}`;
    let authToken: string;
    let chatId: string;
    let postId: string;

    loginSeedUser()
      .then((token) => {
        authToken = token;

        return createChat(authToken, ['Philippe']);
      })
      .then((chatResponse) => {
        chatId = chatResponse.body.chatId;

        return createPost(authToken, caption);
      })
      .then((postResponse) => {
        postId = postResponse.body.postId;

        return cy.request<ChatMessage>({
          method: 'POST',
          url: `${apiUrl}/chats/${chatId}/messages`,
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
          body: {
            content: 'Sharing this with you',
            postIdToShare: postId,
          },
        });
      })
      .then((messageResponse) => {
        expect(messageResponse.status).to.eq(201);
        expect(messageResponse.body.content).to.eq('Sharing this with you');
        expect(messageResponse.body.sharedPost?.postId).to.eq(postId);
        expect(messageResponse.body.sharedPost?.caption).to.eq(caption);

        deletePost(authToken, postId);
        deleteChat(authToken, chatId);
      });
  });

  it('deletes a direct chat', () => {
    let authToken: string;
    let chatId: string;

    loginSeedUser()
      .then((token) => {
        authToken = token;

        return createChat(authToken, ['Chris']);
      })
      .then((chatResponse) => {
        chatId = chatResponse.body.chatId;

        return cy.request({
          method: 'DELETE',
          url: `${apiUrl}/chats/${chatId}/members/me`,
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        });
      })
      .then((deleteResponse) => {
        expect(deleteResponse.status).to.eq(204);

        return getChats(authToken);
      })
      .then((chatsResponse) => {
        expect(chatsResponse.body.some((chat) => chat.chatId === chatId)).to.eq(false);
      });
  });

  it('deletes an owned group chat', () => {
    const groupName = `e2e_delete_group_${Date.now()}`;
    let authToken: string;
    let chatId: string;

    loginSeedUser()
      .then((token) => {
        authToken = token;

        return createChat(authToken, ['Felix', 'Franz'], groupName);
      })
      .then((chatResponse) => {
        chatId = chatResponse.body.chatId;

        return deleteChat(authToken, chatId);
      })
      .then((deleteResponse) => {
        expect(deleteResponse.status).to.eq(204);

        return getChats(authToken);
      })
      .then((chatsResponse) => {
        expect(chatsResponse.body.some((chat) => chat.chatId === chatId)).to.eq(false);
      });
  });

  it('adds and removes members in a group chat', () => {
    const groupName = `e2e_members_group_${Date.now()}`;
    let authToken: string;
    let chatId: string;

    loginSeedUser()
      .then((token) => {
        authToken = token;

        return createChat(authToken, ['Felix'], groupName);
      })
      .then((chatResponse) => {
        chatId = chatResponse.body.chatId;

        return cy.request<Chat>({
          method: 'POST',
          url: `${apiUrl}/chats/${chatId}/members`,
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
          body: {
            usernames: ['Franz'],
          },
        });
      })
      .then((addResponse) => {
        expect(addResponse.status).to.eq(200);
        expect(hasParticipant(addResponse.body, 'Franz')).to.eq(true);

        const felix = addResponse.body.participants.find((participant) => participant.username === 'Felix');
        expect(felix).to.exist;

        return cy.request<Chat>({
          method: 'DELETE',
          url: `${apiUrl}/chats/${chatId}/members/${felix!.id}`,
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        });
      })
      .then((removeResponse) => {
        expect(removeResponse.status).to.eq(200);
        expect(hasParticipant(removeResponse.body, 'Franz')).to.eq(true);
        expect(hasParticipant(removeResponse.body, 'Felix')).to.eq(false);

        deleteChat(authToken, chatId);
      });
  });
});

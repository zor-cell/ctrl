// ***********************************************
// This example namespace declaration will help
// with Intellisense and code completion in your
// IDE or Text Editor.
// ***********************************************
// declare namespace Cypress {
//   interface Chainable<Subject = any> {
//     customCommand(param: any): typeof customCommand;
//   }
// }
//
// function customCommand(param: any): void {
//   console.warn(param);
// }
//
// NOTE: You can use it like so:
// Cypress.Commands.add('customCommand', customCommand);
//
// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })
declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Cypress {
    interface Chainable {
      register(
        overrides?: Partial<{
          username: string;
          password: string;
          email: string;
          name: string;
          birthdate: string;
        }>
      ): Chainable<Cypress.Response<unknown>>;
      fetchVerificationToken(
        overrides?: Partial<{
          username: string;
          email: string;
        }>
      ): Chainable<string>;
      verifyEmail(username: string, token: string): Chainable<Cypress.Response<unknown>>;
      deleteUser(username: string, password: string): Chainable<Cypress.Response<unknown>>;
      fetchResetToken(
        overrides?: Partial<{
          username: string;
          email: string;
        }>
      ): Chainable<string>;
    }
  }
}

Cypress.Commands.add('register', (overrides = {}) => {
  const timestamp = Date.now();
  const defaultUsername = Cypress.env('TEST_USER') || `test_user_${timestamp}`;

  return cy
    .request({
      method: 'POST',
      url: '/api/auth/register',
      body: {
        username: defaultUsername,
        password: Cypress.env('TEST_PASSWORD') || 'TestPassword123!',
        email: Cypress.env('TEST_EMAIL') || `${defaultUsername}@example.com`,
        name: Cypress.env('TEST_NAME') || 'Cypress Test User',
        birthdate: Cypress.env('TEST_BIRTHDATE') || '2000-01-01',
        ...overrides,
      },
    })
    .then((response) => {
      expect(response.status).to.eq(201);
      return response;
    });
});

Cypress.Commands.add('deleteUser', (username, password) => {
  return cy.request({
    method: 'POST',
    url: '/api/auth/login',
    failOnStatusCode: true,
    body: {
      username,
      password,
    },
  }).then((loginResponse) => {
    const jwt: string = loginResponse.body.token;

    return cy.request({
      method: 'DELETE',
      url: '/api/users/me',
      failOnStatusCode: false,
      headers: {
        Authorization: 'Bearer ' + jwt,
      },
    });
  });
});

Cypress.Commands.add('fetchVerificationToken', (overrides = {}) => {
  const timestamp = Date.now();
  const defaultUsername = Cypress.env('TEST_USER') || ('test_user_' + timestamp);

  const targetUsername = overrides.username || defaultUsername;
  const targetEmail = overrides.email || Cypress.env('TEST_EMAIL') || (targetUsername + '@example.com');

  return cy
    .request({
      method: 'POST',
      url: '/api/auth/email',
      failOnStatusCode: true,
      body: {
        username: targetUsername,
        email: targetEmail,
      },
    })
    .then((emailResponse) => {
      console.log('fetchVerificationToken /api/auth/email response:', emailResponse);
      return emailResponse;
    })
    .then(() =>
      cy.request({
        method: 'POST',
        url: '/api/auth/login',
        failOnStatusCode: true,
        body: {
          username: 'TestUser',
          password: 'test12345678',
        },
      })
    )
    .then((loginResponse) => {
      console.log('fetchVerificationToken /api/auth/login response:', loginResponse);
      return loginResponse;
    })
    .then((loginResponse) =>
      cy.request({
        method: 'GET',
        url: '/api/auth/verification-token',
        failOnStatusCode: true,
        headers: {
          Authorization: 'Bearer ' + loginResponse.body.token,
        },
        body: {
          username: targetUsername,
          email: targetEmail,
        },
      })
    )
    .then((tokenResponse) => {
      console.log('fetchVerificationToken /api/auth/verification-token response:', tokenResponse);
      return (tokenResponse.status === 200 ? tokenResponse.body : '000000') as string;
    });
});

Cypress.Commands.add('fetchResetToken', (overrides = {}) => {
  const timestamp = Date.now();
  const defaultUsername = Cypress.env('TEST_USER') || ('test_user_' + timestamp);

  const targetUsername = overrides.username || defaultUsername;
  const targetEmail = overrides.email || Cypress.env('TEST_EMAIL') || (targetUsername + '@example.com');

  return cy
      .request({
        method: 'POST',
        url: '/api/auth/login',
        failOnStatusCode: true,
        body: {
          username: 'TestUser',
          password: 'test12345678',
        },
      })
    .then((loginResponse) => {
      console.log('fetchResetToken /api/auth/login response:', loginResponse);
      return loginResponse;
    })
    .then((loginResponse) =>
      cy.request({
        method: 'GET',
        url: '/api/auth/password-token',
        failOnStatusCode: true,
        headers: {
          Authorization: 'Bearer ' + loginResponse.body.token,
        },
        body: {
          username: targetUsername,
          email: targetEmail,
        },
      })
    )
    .then((tokenResponse) => {
      console.log('fetchVerificationToken /api/auth/password-token response:', tokenResponse);
      return (tokenResponse.status === 200 ? tokenResponse.body : '000000') as string;
    });
});

Cypress.Commands.add('verifyEmail', (username: string, token: string) => {
  return cy.request({
    method: 'POST',
    url: '/api/auth/verify',
    failOnStatusCode: true,
    body: {
      username,
      code: token,
    },
  });
});

export {};

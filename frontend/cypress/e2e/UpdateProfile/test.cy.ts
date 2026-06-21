import { environment } from '../../../src/environments/environment';

const describeProfile = environment.auth.requireEmailVerification ? describe.skip : describe;

type TestUser = {
  username: string;
  password: string;
  email: string;
  name: string;
  birthdate: string;
};

const password = '$TestUser123';

function uniqueUser(prefix: string): TestUser {
  const suffix = `${Date.now() % 100000}${Cypress._.random(10, 99)}`;

  return {
    username: `${prefix}${suffix}`,
    password,
    email: `${prefix}${suffix}@local.test`,
    name: `Update Profile ${suffix}`,
    birthdate: '2000-01-01',
  };
}

function registerUser(user: TestUser): Cypress.Chainable<Cypress.Response<unknown>> {
  return cy.register(user);
}

function loginViaUi(username: string, userPassword = password): void {
  cy.visit('/login');
  cy.get('input#username').should('be.visible').clear().type(username);
  cy.get('input[type="password"]').should('be.visible').clear().type(userPassword);
  cy.contains('button', 'Log In').click();
  cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
}

function cleanupUser(username: string, userPassword = password): Cypress.Chainable<Cypress.Response<unknown> | null> {
  return cy
    .request({
      method: 'POST',
      url: '/api/auth/login',
      failOnStatusCode: false,
      body: {
        username,
        password: userPassword,
      },
    })
    .then((loginResponse) => {
      if (loginResponse.status !== 200) {
        return null;
      }

      return cy.request({
        method: 'DELETE',
        url: '/api/users/me',
        failOnStatusCode: false,
        headers: {
          Authorization: `Bearer ${loginResponse.body.token}`,
        },
      });
    });
}

function openProfileEditor(): void {
  cy.visit('/profile');
  cy.contains('button', 'Edit').should('be.visible').click();
  cy.contains('.p-dialog', 'Edit profile').should('be.visible');
}

describeProfile('Update Profile critical flows', () => {
  it('updates username and biography from the profile edit dialog', () => {
    const user = uniqueUser('puser');
    const updatedUsername = `${user.username}n`;
    const updatedBio = `Updated biography for ${updatedUsername}`;

    registerUser(user);
    loginViaUi(user.username);
    openProfileEditor();

    const usernameInput = '.p-dialog input.profile-edit-input';

    cy.get(usernameInput).should('be.visible').click().type('{selectall}{backspace}');
    cy.get(usernameInput).should('be.visible').type(updatedUsername, { delay: 20 });
    cy.get(usernameInput).should('have.value', updatedUsername);
    cy.contains('Username is available.', { timeout: 5000 }).should('be.visible');

    cy.get('.p-dialog textarea[aria-label="Edit bio"]')
      .should('be.visible')
      .clear()
      .type(updatedBio);

    cy.intercept('PUT', '/api/users/me').as('updateProfile');
    cy.intercept('PUT', '/api/users/me/username').as('updateUsername');
    cy.contains('.p-dialog button', 'Save').should('not.be.disabled').click();

    cy.wait('@updateProfile').its('response.statusCode').should('eq', 200);
    cy.wait('@updateUsername').its('response.statusCode').should('eq', 200);

    cy.contains('.p-dialog', 'Edit profile').should('not.exist');
    cy.visit('/profile');
    cy.get('.post-profile-card h1', { timeout: 10000 }).should('contain.text', updatedUsername);
    cy.get('.profile-bio').should('contain.text', updatedBio);

    cleanupUser(updatedUsername);
  });

  it('updates the profile picture from the profile edit dialog', () => {
    const user = uniqueUser('ppic');
    const pngBytes = Cypress.Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
      'base64',
    );

    registerUser(user);
    loginViaUi(user.username);
    openProfileEditor();

    cy.get('input[type="file"][accept="image/*"]').selectFile(
      {
        contents: pngBytes,
        fileName: 'profile-picture.png',
        mimeType: 'image/png',
      },
      { force: true },
    );

    cy.get('.p-dialog .profile-avatar__image').should('be.visible');

    cy.intercept('PUT', '/api/users/me').as('updateProfile');
    cy.intercept('PUT', '/api/users/me/profile-picture').as('updateProfilePicture');
    cy.contains('.p-dialog button', 'Save').should('not.be.disabled').click();

    cy.wait('@updateProfile').its('response.statusCode').should('eq', 200);
    cy.wait('@updateProfilePicture').its('response.statusCode').should('eq', 200);

    cy.contains('.p-dialog', 'Edit profile').should('not.exist');
    cy.get('.post-profile-card .profile-avatar--display img.profile-avatar__image', {
      timeout: 10000,
    }).should('be.visible');

    cleanupUser(user.username);
  });

  it('deletes the current account and prevents logging in again', () => {
    const user = uniqueUser('deleteprofile');

    registerUser(user);
    loginViaUi(user.username);

    cy.visit('/settings?tab=account');
    cy.contains('.account-item--danger button', 'Delete account').should('be.visible').click();
    cy.contains('.p-dialog', 'Delete account?').should('be.visible');

    cy.intercept('DELETE', '/api/users/me').as('deleteAccount');
    cy.contains('.p-dialog button', 'Delete account').should('be.visible').click();

    cy.wait('@deleteAccount').its('response.statusCode').should('eq', 204);
    cy.url().should('eq', `${Cypress.config().baseUrl}/login`);

    cy.request({
      method: 'POST',
      url: '/api/auth/login',
      failOnStatusCode: false,
      body: {
        username: user.username,
        password: user.password,
      },
    }).its('status').should('eq', 401);
  });
});

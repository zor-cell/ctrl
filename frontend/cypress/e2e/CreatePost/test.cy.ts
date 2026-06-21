import { environment } from '../../../src/environments/environment';

const describePost = environment.auth.requireEmailVerification ? describe.skip : describe;


type TestUser = {
  username: string;
  password: string;
  email: string;
  name: string;
  birthdate: string;
};

const password = '$TestUser123';
const verifiedUser = {
  username: 'TestUser',
  password: 'test12345678',
};


function uniqueUser(): TestUser {
  const suffix = `${Date.now() % 100000}${Cypress._.random(10, 99)}`;

  return {
    username: `postu${suffix}`,
    password,
    email: `postu${suffix}@local.test`,
    name: `Post User ${suffix}`,
    birthdate: '2000-01-01',
  };
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

function openComposerDetails(): void {
  cy.contains('button', 'Create Post').should('be.visible').click();
  cy.contains('.p-dialog', 'Create Post').should('be.visible');
  cy.contains('button', 'Next').should('be.visible').click();
  cy.contains('.p-dialog', 'Add post details').should('be.visible');
}

function submitComposer(): void {
  cy.contains('.p-dialog button', 'Create Post')
    .scrollIntoView()
    .should('be.visible')
    .click();
}

function fillCaption(caption: string): void {
  cy.get('.p-dialog textarea.field__textarea')
    .should('be.visible')
    .clear()
    .type(caption);
}

function enterCaptionValue(caption: string): void {
  cy.get('.p-dialog textarea.field__textarea')
    .should('be.visible')
    .clear()
    .invoke('val', caption)
    .trigger('input')
    .trigger('change');
}

function fillFirstSource(source: { title?: string; author?: string; url?: string; journal?: string }): void {
  if (source.journal) {
    cy.get('.p-dialog input[id="journal-0"]')
      .scrollIntoView()
      .should('be.visible')
      .clear()
      .type(source.journal);
  }

  if (source.author) {
    cy.get('.p-dialog input[id="author-0"]')
      .scrollIntoView()
      .should('be.visible')
      .clear()
      .type(source.author);
  }

  if (source.title) {
    cy.get('.p-dialog input[id="title-0"]')
      .scrollIntoView()
      .should('be.visible')
      .clear()
      .type(source.title);
  }

  if (source.url) {
    cy.get('.p-dialog input[id="url-0"]')
      .scrollIntoView()
      .should('be.visible')
      .clear()
      .type(source.url);
  }
}

function createTextPost(caption: string, visibility: 'private' | 'public'): void {
  openComposerDetails();

  fillCaption(caption);

  if (visibility === 'public') {
    cy.contains('.visibility-toggle', 'Private').within(() => {
      cy.get('input#isPublic').click({ force: true });
    });
    cy.contains('.visibility-toggle', 'Public').should('be.visible');
  } else {
    cy.get('body').then(($body) => {
      if ($body.find('.visibility-toggle').length > 0) {
        cy.contains('.visibility-toggle', 'Private').should('be.visible');
      } else {
        cy.contains('Because your account is not verified, this post will be saved as private.')
          .should('be.visible');
      }
    });
  }

  cy.intercept('POST', '/api/posts').as('createPost');
  submitComposer();
  cy.wait('@createPost').its('response.statusCode').should('eq', 201);
  cy.contains('.p-dialog', 'Create Post').should('not.exist');
}

function openPostFromProfile(caption: string): void {
  cy.visit('/profile');
  cy.contains('.post-card', caption, { timeout: 10000 }).should('be.visible').click();
  cy.contains('.post-caption', caption, { timeout: 10000 }).should('be.visible');
}

describePost('Create Post critical flows', () => {
  it('creates a private text post', () => {
    const user = uniqueUser();
    const caption = 'PrivatePostFlow';

    cy.register(user);
    loginViaUi(user.username);

    createTextPost(caption, 'private');

    cy.contains('.post-card', caption, { timeout: 10000 }).should('be.visible');
    cy.contains('.post-card', caption)
      .find('[aria-label="Private post"]')
      .should('exist');

    cleanupUser(user.username);
  });

  it('creates a public text post', () => {
    const caption = 'PublicPostFlow';

    loginViaUi(verifiedUser.username, verifiedUser.password);

    createTextPost(caption, 'public');

    cy.contains('.post-card', caption, { timeout: 10000 }).should('be.visible');
    cy.contains('.post-card', caption)
      .find('[aria-label="Public post"]')
      .should('exist');

    openPostFromProfile(caption);
    cy.get('button[aria-label="Delete post"]').should('be.visible').click();
    cy.contains('.p-dialog', 'Delete post').should('be.visible');
    cy.contains('.p-dialog button', 'Delete').should('be.visible').click();
    cy.contains('.post-caption', caption).should('not.exist');
  });

  it('deletes an own post from the post detail dialog', () => {
    const user = uniqueUser();
    const caption = 'DeletePostFlow';

    cy.register(user);
    loginViaUi(user.username);
    createTextPost(caption, 'private');

    openPostFromProfile(caption);
    cy.get('button[aria-label="Delete post"]').should('be.visible').click();
    cy.contains('.p-dialog', 'Delete post').should('be.visible');

    cy.intercept('DELETE', '/api/posts/*').as('deletePost');
    cy.contains('.p-dialog button', 'Delete').should('be.visible').click();
    cy.wait('@deletePost').its('response.statusCode').should('be.oneOf', [200, 204]);

    cy.contains('.post-caption', caption).should('not.exist');
    cy.contains('.post-card', caption).should('not.exist');

    cleanupUser(user.username);
  });

  it('keeps post creation blocked when caption is missing', () => {
    const user = uniqueUser();

    cy.register(user);
    loginViaUi(user.username);
    openComposerDetails();

    cy.intercept('POST', '/api/posts').as('createPost');
    submitComposer();

    cy.contains('Caption is required.').scrollIntoView().should('be.visible');
    cy.get('@createPost.all').should('have.length', 0);
    cy.contains('.p-dialog', 'Create Post').should('be.visible');

    cleanupUser(user.username);
  });

  it('keeps post creation blocked when caption contains only whitespace', () => {
    const user = uniqueUser();

    cy.register(user);
    loginViaUi(user.username);
    openComposerDetails();
    enterCaptionValue('   ');

    cy.intercept('POST', '/api/posts').as('createPost');
    submitComposer();

    cy.contains('Caption must contain text.').scrollIntoView().should('be.visible');
    cy.get('@createPost.all').should('have.length', 0);
    cy.contains('.p-dialog', 'Create Post').should('be.visible');

    cleanupUser(user.username);
  });

  it('creates a post with a source', () => {
    const user = uniqueUser();
    const caption = 'SourcePostFlow';
    const sourceTitle = 'E2E Source Title';
    const sourceAuthor = 'E2E Source Author';

    cy.register(user);
    loginViaUi(user.username);
    openComposerDetails();

    fillCaption(caption);
    fillFirstSource({
      title: sourceTitle,
      author: sourceAuthor,
      journal: 'E2E Journal',
    });

    cy.intercept('POST', '/api/posts').as('createPost');
    submitComposer();
    cy.wait('@createPost').its('response.statusCode').should('eq', 201);

    openPostFromProfile(caption);
    cy.contains('.src-toggle', 'Sources').should('be.visible').click();
    cy.contains('.src-card-title', sourceTitle).should('be.visible');
    cy.contains('.src-meta', sourceAuthor).should('be.visible');

    cleanupUser(user.username);
  });

  it('shows a source title error when source details are entered without a title', () => {
    const user = uniqueUser();

    cy.register(user);
    loginViaUi(user.username);
    openComposerDetails();

    fillCaption('InvalidSourceFlow');
    fillFirstSource({
      author: 'E2E Source Author',
    });

    cy.intercept('POST', '/api/posts').as('createPost');
    submitComposer();

    cy.contains('Title is required when you add source information.').should('be.visible');
    cy.get('@createPost.all').should('have.length', 0);
    cy.contains('.p-dialog', 'Create Post').should('be.visible');

    cleanupUser(user.username);
  });

  it('shows a source author or url error when source title is entered alone', () => {
    const user = uniqueUser();

    cy.register(user);
    loginViaUi(user.username);
    openComposerDetails();

    fillCaption('MissingSourceAuthorFlow');
    fillFirstSource({
      title: 'E2E Source Title',
    });

    cy.intercept('POST', '/api/posts').as('createPost');
    submitComposer();

    cy.contains('Add either an author or a URL for this source.').should('be.visible');
    cy.get('@createPost.all').should('have.length', 0);
    cy.contains('.p-dialog', 'Create Post').should('be.visible');

    cleanupUser(user.username);
  });
});

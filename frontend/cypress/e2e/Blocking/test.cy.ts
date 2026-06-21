import { environment } from '../../../src/environments/environment';

const describePasswordReset = environment.auth.requireEmailVerification ? describe.skip : describe;

interface TestUser {
  username: string;
  password: string;
  email: string;
  name: string;
  birthdate: string;
}

describePasswordReset('Blocking Tests', () => {
  let blockedUser: TestUser;
  let blockingUser: TestUser;

  const adminUser = {
    username: 'admin',
    password: 'ctrl-test-admin-pw',
  };

  beforeEach(function () {
    const timestamp = Date.now();

    blockedUser = {
      username: `BlockedUser_${timestamp}`,
      password: '$TestUser123',
      email: `blocked.user.${timestamp}@local.test`,
      name: 'BlockedUser',
      birthdate: '2000-01-01',
    };

    blockingUser = {
      username: `BlockingUser_${timestamp}`,
      password: '$TestUser123',
      email: `blocking.user.${timestamp}@local.test`,
      name: 'BlockedUser',
      birthdate: '2001-01-01',
    };

    cy
      .register(blockingUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);
      });
    cy
      .register(blockedUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);

      });
  });

  afterEach(function () {
    cy.deleteUser(blockedUser.username, blockedUser.password).then((res) => {
      expect(res.status).to.eq(204);
    });

    cy.deleteUser(blockingUser.username, blockingUser.password).then((res) => {
      expect(res.status).to.eq(204);
    });
  })

  it ("If user blocks other user, other user should be under Blocked", () => {
    cy.visit('/login');
    cy.get('input#username').type(blockingUser.username);
    cy.get('input[type="password"]').type(blockingUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit(`/profile/${blockedUser.username}`);
    cy.get('[icon="pi pi-ellipsis-v"] > .p-ripple').should("be.visible").click();
    cy.get('#pn_id_43_1 > .p-menu-item-content > .p-ripple').should("be.visible").click();

    cy.contains("button", "Block").should("be.visible").click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/followed?tab=blocked`);

    cy.contains(blockedUser.username).should("be.visible")

  });

  it ("If user blocks other user, other user should not be viewable in detail view", () => {
    cy.visit('/login');
    cy.get('input#username').type(blockingUser.username);
    cy.get('input[type="password"]').type(blockingUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit(`/profile/${blockedUser.username}`);
    cy.get('[icon="pi pi-ellipsis-v"] > .p-ripple').should("be.visible").click();
    cy.get('#pn_id_43_1 > .p-menu-item-content > .p-ripple').should("be.visible").click();

    cy.contains("button", "Block").should("be.visible").click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/followed?tab=blocked`);

    cy.visit(`/profile/${blockedUser.username}`);
    cy.contains("User not found")

  });

  it ("If user blocks other user, other user no longer be searchable", () => {
    cy.visit('/login');
    cy.get('input#username').type(blockingUser.username);
    cy.get('input[type="password"]').type(blockingUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/search/users")
    cy.get('input[type="text"]').should("be.visible").type(blockedUser.username);
    cy.contains(blockedUser.username).should("be.visible");

    cy.visit(`/profile/${blockedUser.username}`);
    cy.get('[icon="pi pi-ellipsis-v"] > .p-ripple').should("be.visible").click();
    cy.get('#pn_id_43_1 > .p-menu-item-content > .p-ripple').should("be.visible").click();

    cy.contains("button", "Block").should("be.visible").click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/followed?tab=blocked`);

    cy.contains(blockedUser.username).should("be.visible")

    cy.visit("/search/users")
    cy.get('input[type="text"]').should("be.visible").type(blockedUser.username);
    cy.contains(blockedUser.username).should("not.exist");

  });

  it ("If user blocks follower, follow should be removed", () => {
    cy.createFriendship(blockedUser.username, blockedUser.password, blockingUser.username, blockingUser.password).then((response) => {
      expect(response.status).to.eq(204);

      cy.visit('/login');
      cy.get('input#username').type(blockingUser.username);
      cy.get('input[type="password"]').type(blockingUser.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

      cy.visit(`/profile/${blockedUser.username}`);
      cy.get('[icon="pi pi-ellipsis-v"] > .p-ripple').should("be.visible").click();
      cy.get('#pn_id_43_1 > .p-menu-item-content > .p-ripple').should("be.visible").click();

      cy.contains("button", "Block").should("be.visible").click();

      cy.url().should('eq', `${Cypress.config().baseUrl}/followed?tab=blocked`);

      cy.visit("/followed?tab=followed");
      cy.contains(blockedUser.username).should("not.exist")
    });
  });

  it ("If user blocks friend, friendship should get removed and comments deleted", () => {
    cy.createFriendship(blockedUser.username, blockedUser.password, blockingUser.username, blockingUser.password).then((response) => {
      expect(response.status).to.eq(204);

      cy.visit('/login');
      cy.get('input#username').type(blockingUser.username);
      cy.get('input[type="password"]').type(blockingUser.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

      cy.visit("/post")
      cy.contains("button", "Weiter").should("be.visible").click();
      cy.get('textarea').should("be.visible").invoke('val', 'Test comment')
        .trigger('input')
        .trigger('change');

      cy.get('.p-dialog-content').should('be.visible').scrollTo('bottom');
      cy.contains("button", "Create Post").scrollIntoView().should("be.visible").click();

      cy.contains("Test comment").should("be.visible");

      cy.visit('/login');
      cy.get('input#username').type(blockedUser.username);
      cy.get('input[type="password"]').type(blockedUser.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

      cy.visit(`/profile/${blockingUser.username}`);

      cy.contains("button", "Test comment").should("be.visible").click();

      cy.get('textarea').should("be.visible")
        .invoke('val', 'This is a test comment')
        .trigger('input')
        .trigger('change');
      cy.get('.comment-input-row').find('button').click();

      cy.visit('/login');
      cy.get('input#username').type(blockingUser.username);
      cy.get('input[type="password"]').type(blockingUser.password);
      cy.get('button[type="submit"]').click();
      cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

      cy.visit(`/profile/${blockedUser.username}`);
      cy.get('[icon="pi pi-ellipsis-v"] > .p-ripple').should("be.visible").click();
      cy.get('#pn_id_43_1 > .p-menu-item-content > .p-ripple').should("be.visible").click();

      cy.contains("button", "Block").should("be.visible").click();

      cy.url().should('eq', `${Cypress.config().baseUrl}/followed?tab=blocked`);

      cy.contains(blockedUser.username).should("be.visible")

      cy.visit("/followed?tab=friends");
      cy.contains(blockedUser.username).should("not.exist");

      cy.visit(`/profile/${blockingUser.username}`);

      cy.contains("button", "Test comment").should("be.visible").click();

      cy.contains("This is a test comment").should("not.exist");
    });

  });

  it ("If user blocks other user, users post should not be findable", () => {

    cy.visit('/login');
    cy.get('input#username').type(blockingUser.username);
    cy.get('input[type="password"]').type(blockingUser.password);
    cy.get('button[type="submit"]').click();

    cy.get('button[aria-label="Open profile settings"]')
      .should('be.visible')
    .click();

  cy.contains('button', 'Verify Now')
    .should('be.visible')
    .click();

  cy.contains('button', 'Business Account')
    .should('be.visible')
    .click();

    cy.visit('/login')

    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_1 > .p-menubar-item-content > .p-ripple').click();
    cy.contains(blockingUser.username)
    cy.contains(blockingUser.email)
    cy.contains(blockingUser.name)

    cy.contains("button", "Approve")
      .should("be.visible")
      .click();

    cy.visit('/login');
    cy.get('input#username').type(blockingUser.username);
    cy.get('input[type="password"]').type(blockingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/post")
    cy.contains("button", "Weiter").should("be.visible").click();
    cy.get('textarea').should("be.visible").invoke('val', 'This is a test')
      .trigger('input')
      .trigger('change');

    cy.get('#isPublic').click();

    cy.get('.p-dialog-content').should('be.visible').scrollTo('bottom');
    cy.contains("button", "Create Post").scrollIntoView().should("be.visible").click();

    cy.contains("This is a test").should("be.visible");

    cy.visit(`/profile/${blockedUser.username}`);
    cy.get('[icon="pi pi-ellipsis-v"] > .p-ripple').should("be.visible").click();
    cy.get('#pn_id_43_1 > .p-menu-item-content > .p-ripple').should("be.visible").click();

    cy.contains("button", "Block").should("be.visible").click();

    cy.contains(blockedUser.username).should("be.visible")


    cy.visit('/login');
    cy.get('input#username').type(blockedUser.username);
    cy.get('input[type="password"]').type(blockedUser.password);
    cy.get('button[type="submit"]').click();
    cy.visit("/search/posts")
    cy.get('input[type="text"]').should("be.visible").type("This is a test");
    cy.contains("button","Search").should("be.visible").click();
    cy.contains(blockedUser.username).should("not.exist");

  });

});

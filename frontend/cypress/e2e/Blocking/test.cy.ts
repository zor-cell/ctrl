import { environment } from '../../../src/environments/environment';

const describePasswordReset = environment.auth.requireEmailVerification ? describe.skip : describe;

describePasswordReset('Blocking Tests', () => {
  const blockedUser = {
    username: 'BlockedUser',
    password: '$TestUser123',
    email: 'blocked.user@local.test',
    name: 'BlockedUser',
    birthdate: '2000-01-01',
  };

  const blockingUser = {
    username: 'BlockingUser',
    password: '$TestUser123',
    email: 'blocking.user@local.test',
    name: 'BlockedUser',
    birthdate: '2001-01-01',
  };

  beforeEach(function () {
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
    cy.deleteUser(blockingUser.username, blockingUser.password).then((res) => {
      expect(res.status).to.eq(204);
    });

    cy.deleteUser(blockedUser.username, blockedUser.password).then((res) => {
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

  it.only ("If user blocks other user, other user should not be viewable in detail view", () => {
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

  it ("If user blocks other user, other user no longer by searchable", () => {
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
      cy.get('.comment-input-row > p-button > .p-ripple').click();

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

});

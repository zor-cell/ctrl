describe('Business Verification Tests', () => {
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

        return cy.fetchVerificationToken({
          username: blockingUser.username,
          email: blockingUser.email,
        });
      })
      .then((token) => cy.verifyEmail(blockingUser.username, token))
      .then((response) => {
        expect(response.status).to.eq(200);
      });
    cy
      .register(blockedUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);

        return cy.fetchVerificationToken({
          username: blockedUser.username,
          email: blockedUser.email,
        });
      })
      .then((token) => cy.verifyEmail(blockedUser.username, token))
      .then((response) => {
        expect(response.status).to.eq(200);
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

});


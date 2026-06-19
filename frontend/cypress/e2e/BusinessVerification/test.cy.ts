describe('Business Verification Tests', () => {
  const testUser = {
    username: 'testuserLoginNew',
    password: '$TestUser123',
    email: 'e2e.user@example.com',
    name: 'TestUserNew',
    birthdate: '2000-01-01',
  };

  const adminUser = {
    username: 'admin',
    password: 'ctrl-test-admin-pw',
  };

  beforeEach(function () {
    return cy
      .register(testUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);

        return cy.fetchVerificationToken({
          username: testUser.username,
          email: testUser.email,
        });
      })
      .then((token) => cy.verifyEmail(testUser.username, token))
      .then((response) => {
        expect(response.status).to.eq(200);
      });
  });

  afterEach(function () {
    cy.deleteUser(testUser.username, testUser.password).then((res) => {
      expect(res.status).to.eq(204);
    });
  })

  it("User creates business verification, should display pending", () => {
    cy.visit('/login');
    cy.get('input#username').type(testUser.username);
    cy.get('input[type="password"]').type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.get('button[aria-label="Open profile settings"]')
      .should('be.visible')
      .click();

    cy.contains('button', 'Verify Now')
      .should('be.visible')
      .click();

    cy.contains('button', 'Business Account')
      .should('be.visible')
      .click();

    cy.contains("Verification pending")


  });

  it("User creates business verification, admin approves", () => {
    cy.visit('/login');
    cy.get('input#username').type(testUser.username);
    cy.get('input[type="password"]').type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.get('button[aria-label="Open profile settings"]')
      .should('be.visible')
      .click();

    cy.contains('button', 'Verify Now')
      .should('be.visible')
      .click();

    cy.contains('button', 'Business Account')
      .should('be.visible')
      .click();

    cy.contains("Verification pending")

    cy.visit('/login')

    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_1 > .p-menubar-item-content > .p-ripple').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/business-verification`);
    cy.contains(testUser.username)
    cy.contains(testUser.email)
    cy.contains(testUser.name)

    cy.contains("button", "Approve")
      .should("be.visible")
      .click();

    cy.visit('/login');
    cy.get('input#username').type(testUser.username);
    cy.get('input[type="password"]').type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.get('button[aria-label="Open profile settings"]')
      .should('be.visible')
      .click();

    cy.contains("Verified as business account")
  });

  it("User creates business verification, admin denies", () => {
    cy.visit('/login');
    cy.get('input#username').type(testUser.username);
    cy.get('input[type="password"]').type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.get('button[aria-label="Open profile settings"]')
      .should('be.visible')
      .click();

    cy.contains('button', 'Verify Now')
      .should('be.visible')
      .click();

    cy.contains('button', 'Business Account')
      .should('be.visible')
      .click();

    cy.contains("Verification pending")

    cy.visit('/login')

    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_1 > .p-menubar-item-content > .p-ripple').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/business-verification`);
    cy.contains(testUser.username)
    cy.contains(testUser.email)
    cy.contains(testUser.name)

    cy.contains("button", "Deny")
      .should("be.visible")
      .click();

    cy.visit('/login');
    cy.get('input#username').type(testUser.username);
    cy.get('input[type="password"]').type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.get('button[aria-label="Open profile settings"]')
      .should('be.visible')
      .click();

    cy.contains("Your verification was rejected")
  });
});

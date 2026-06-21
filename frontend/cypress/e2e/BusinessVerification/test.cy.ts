import { environment } from '../../../src/environments/environment';

const describePasswordReset = environment.auth.requireEmailVerification ? describe.skip : describe;

interface TestUser {
  username: string;
  password: string;
  email: string;
  name: string;
  birthdate: string;
}

describePasswordReset('Business Verification Tests', () => {
  let testUser: TestUser;

  const adminUser = {
    username: Cypress.env("TEST_ADMIN"),
    password: Cypress.env("TEST_ADMIN_PASSWORD"),
  };

  beforeEach(function () {
    const timestamp = Date.now();

    testUser = {
      username: `testuserLoginNew_${timestamp}`,
      password: '$TestUser123',
      email: `e2e.user.${timestamp}@example.com`,
      name: 'TestUserNew',
      birthdate: '2000-01-01',
    };

    return cy
      .register(testUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);
      });
  });

  const logoutBeforeLogin = () => {
    cy.clearCookies();
    cy.clearLocalStorage();
    cy.visit('/login');
  };

  afterEach(function () {
    cy.deleteUser(testUser.username, testUser.password).then((res) => {
      expect(res.status).to.eq(204);
    });
  })

  it("User creates business verification, should display pending", () => {
    logoutBeforeLogin();
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
    logoutBeforeLogin();
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

    logoutBeforeLogin();

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

    logoutBeforeLogin();
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
    logoutBeforeLogin();
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

    logoutBeforeLogin();

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

    logoutBeforeLogin();
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

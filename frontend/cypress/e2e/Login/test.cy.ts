import { environment } from '../../../src/environments/environment';

const describeLogin = environment.auth.requireEmailVerification ? describe.skip : describe;

describeLogin('Login Loaded', () => {
  const testUser = {
    username: 'testuserLoginNew',
    password: '$TestUser123',
    email: 'e2e.user@example.com',
    name: 'TestUserNew',
    birthdate: '2000-01-01',
  };

  before(function () {
    return cy
      .register(testUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);
      });
  });

  after(function () {
    cy.deleteUser(testUser.username, testUser.password).then((res) => {
      expect(res.status).to.eq(204);
    });
  })

  it('Visits the initial project page', () => {
    cy.visit('/login');
    cy.contains('Welcome Back');
  });

  it('should have username and password fields', () => {
    cy.visit('/login');

    cy.get('input#username').should("be.visible")
    cy.get('input[type="password"]').should("be.visible")
  })

  it("should have link to register", () => {
    cy.visit('/login');
    cy.contains('a', 'Register here').should("be.visible").and("have.attr", "href", "/register");
  })

  it("should have link to forgot password", () => {
    cy.visit('/login');
    cy.contains('a', 'Forgot Password?').should("be.visible").and("have.attr", "href", "/forgot-password");
  })

  it("should login existing user successfully", () => {
    cy.visit('/login');
    cy.get('input#username').type(testUser.username);
    cy.get('input[type="password"]').type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
  })

  it("should display invalid username or password on wrong password", () => {
    cy.visit('/login');
    cy.get('input#username').type(testUser.username);
    cy.get('input[type="password"]').type("wrongPassword");
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/login`);
    cy.contains('Invalid username or password').should("be.visible");
  })

  it("should display invalid username or password on wrong username", () => {
    cy.visit('/login');
    cy.get('input#username').type("wrongUsername");
    cy.get('input[type="password"]').type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/login`);
    cy.contains('Invalid username or password').should("be.visible");
  })

  it("should display username is required if login with no username", () => {
    cy.visit('/login');
    cy.get('input[type="password"]').type(testUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/login`);
    cy.contains('Username is required').should("be.visible");
  })

  it("should display username is required if login with no password", () => {
    cy.visit('/login');
    cy.get('input#username').type(testUser.username);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/login`);
    cy.contains('Password is required').should("be.visible");
  })
});

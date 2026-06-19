describe('Reset Email Tests', () => {
  const testUser = {
    username: 'testuserLoginNew',
    password: '$TestUser123',
    email: 'e2e.user@example.com',
    name: 'TestUserNew',
    birthdate: '2000-01-01',
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

  it("Reset Password with non existent email should show sent email", () => {
    cy.visit('/login');
    cy.contains('a', 'Forgot Password?').should("be.visible").and("have.attr", "href", "/forgot-password").click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/forgot-password`);
    cy.get('input#email').type("non.existent@gmail.com");
    cy.get("button[type='submit']").click();

    cy.contains("Password reset sent successfully if correct email was provided. Check your emails.")

    cy.url().should('eq', `${Cypress.config().baseUrl}/login`);

  });

  it("Reset password of existent user, should set new password", () => {
    cy.visit("/forgot-password")
    cy.get('input#email').type(testUser.email);
    cy.get("button[type='submit']").click();

    cy.contains("Password reset sent successfully if correct email was provided. Check your emails.")

    cy.url().should('eq', `${Cypress.config().baseUrl}/login`);

    cy.fetchResetToken({username: testUser.username,
      email: testUser.email,}).then((token) => {
        cy.visit(`/reset-password?token=${token}`)
        cy.get('#password').type("NewPassword123!");
        cy.get("#confirmPassword").type("NewPassword123!");
        cy.get("button[type='submit']").click();

        cy.contains("Password reset successfully. You can now log in with your new password.")

        testUser.password= "NewPassword123!"

        cy.url().should('eq', `${Cypress.config().baseUrl}/login`);

        cy.get('input#username').type(testUser.username);
        cy.get('input[type="password"]').type("NewPassword123!");
        cy.get('button[type="submit"]').click();
        cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    });

  });
})

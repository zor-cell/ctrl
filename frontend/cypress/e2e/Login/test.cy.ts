describe('Login Loaded', () => {
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
});

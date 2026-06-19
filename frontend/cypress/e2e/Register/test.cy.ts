describe('Register', () => {

  const testUser = {
    username: 'TestUser.New1',
    password: '$TestUser123',
    email: 'e2e.user@example.com',
    name: 'TestUserNew',
    birthdate: '01.01.2000',
  };

  const testUserForRequest = {
    username: 'TestUser.New1',
    password: '$TestUser123',
    email: 'e2e.user@example.com',
    name: 'TestUserNew',
    birthdate: '2000-01-01',
  };

  const usernameValidationMessage =
    'Username must be at least 3 characters, contain at least one letter or number, and may only use letters, numbers, underscores and dots.';
  const passwordValidationMessage =
    'Password must be at least 8 characters, contain at least one uppercase letter, one lowercase letter, one number and one special character of (!@#$%^&*()_+-=[]{};:,.?). ';
  const invalidPasswordCases = ['invalid', 'invalid123', '!invalid', '!invalid123', '!INVALID123'];

  const fillRegisterForm = (overrides: Partial<typeof testUser> = {}) => {
    const user = { ...testUser, ...overrides };

    cy.get('input#username').type(user.username);
    cy.get('input#email').type(user.email);
    cy.get('input[type="password"]').type(user.password);
    cy.get('input#name').type(user.name);
    cy.get('input#birthdate').click().clear().type(user.birthdate + '{enter}').blur();
  };

  let userWasCreated = false;
  let verified = false

  afterEach(() => {
    if (!userWasCreated) return;

    if (verified) {
      return cy.deleteUser(testUser.username, testUser.password)
        .then(() => {
          userWasCreated = false;
          verified = false;
          return;
        });
    } else {
      return cy.fetchVerificationToken({
        username: testUser.username,
        email: testUser.email,
      })
        .then((token) => cy.verifyEmail(testUser.username, token))
        .then(() => cy.deleteUser(testUser.username, testUser.password))
        .then(() => userWasCreated = false);
    }
  });


  it("Register with new user, redirects to email-verification", () => {
    cy.visit('/register');

    fillRegisterForm();

    cy.get('button[type="submit"]').click();

    userWasCreated = true;

    cy.url().should('eq', `${Cypress.config().baseUrl}/email_verification/${testUser.username}/${testUser.email}`);
    cy.contains("We sent a verification link to").should('be.visible')
    cy.contains(testUser.email).should('be.visible')
    cy.contains("Verification email sent successfully if correct email was provided. Check your emails.").should("be.visible")
    cy.contains("Registered user").should('be.visible')
  });

  it("Register with existing username, should fail to register", () => {
    cy.register(testUserForRequest).then((registerResponse) => {

      expect(registerResponse.status).to.eq(201);
      userWasCreated = true;

      cy.visit('/register');

      fillRegisterForm({ email: 'newtestemail@test.local' });

      cy.get('button[type="submit"]').click();

      cy.url().should('eq', `${Cypress.config().baseUrl}/register`)
      cy.contains(`User with username '${testUser.username}' already exists.`).should("be.visible");
      //cy.contains()
    });
  });

    it("Register with existing email, should fail to register", () => {
      cy.register(testUserForRequest).then((registerResponse) => {

        expect(registerResponse.status).to.eq(201);
        userWasCreated = true;

        cy.visit('/register');

        fillRegisterForm({ username: testUser.username + 'new' });

        cy.get('button[type="submit"]').click();

        cy.url().should('eq', `${Cypress.config().baseUrl}/register`);
        cy.contains(`User with email '${testUser.email}' already exists.`).should("be.visible");
        //cy.contains()
      });
  });

  it("Register With 2 letter username, should have disabled submit button", () => {
    cy.visit('/register');

    fillRegisterForm({ username: 'ab' });

    cy.contains(usernameValidationMessage).should("be.visible");
    cy.get('button[type="submit"]').should("be.disabled");

  });

  it("Register With Wrong Character username, should have disabled submit button", () => {
    cy.visit('/register');

    fillRegisterForm({ username: testUser.username + '!' });

    cy.contains(usernameValidationMessage).should("be.visible");
    cy.get('button[type="submit"]').should("be.disabled");

  });

  it("Register With invalid email, should have disabled submit button", () => {
    cy.visit('/register');

    fillRegisterForm({ email: testUser.email.split('@')[0] });

    cy.contains("Email is required").should("be.visible");
    cy.get('button[type="submit"]').should("be.disabled");

  });

  invalidPasswordCases.forEach((invalidPassword) => {
    it(`Register with invalid password "${invalidPassword}", should have disabled submit button`, () => {
      cy.visit('/register');

      fillRegisterForm({ password: invalidPassword });

      cy.contains(passwordValidationMessage).should('be.visible');
      cy.get('button[type="submit"]').should('be.disabled');
    });
  });

  it("Register should generate token", () => {
    cy.visit('/register');
    fillRegisterForm();

    cy.get('button[type="submit"]').click();

    userWasCreated = true;

    cy.url().should('eq', `${Cypress.config().baseUrl}/email_verification/${testUser.username}/${testUser.email}`);

    cy.wait(3000)

    cy.request({
      method: 'POST',
      url: '/api/auth/login',
      failOnStatusCode: true,
      body: {
        username: 'TestUser',
        password: 'test12345678',
      },
    })
  .then((loginResponse) => {
      console.log('fetchVerificationToken /api/auth/login response:', loginResponse);
      return loginResponse;
    })
      .then((loginResponse) =>
        cy.request({
          method: 'GET',
          url: '/api/auth/verification-token',
          failOnStatusCode: true,
          headers: {
            Authorization: 'Bearer ' + loginResponse.body.token,
          },
          body: {
            username: testUser.username,
            email: testUser.email,
          },
        })
      )
      .then((tokenResponse) => {
        console.log('fetchVerificationToken /api/auth/verification-token response:', tokenResponse);
        expect(tokenResponse.status).to.eq(200);

        cy.get("input#code").type(tokenResponse.body);
        cy.get("button[type='submit']").click();

        cy.url().should('eq', `${Cypress.config().baseUrl}/login`);
        verified = true;
      });
  });


});

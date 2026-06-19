describe('EmailVerification', () => {
  const testUser = {
    username: 'testuserLoginNew',
    password: '$TestUser123',
    email: 'e2e.user@example.com',
    name: 'TestUserNew',
    birthdate: '2000-01-01',
  };

  let verified = false;

  beforeEach(() => {
    cy.register(testUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);
      });
  });

  afterEach(() => {
    if (verified) {
      return cy.deleteUser(testUser.username, testUser.password)
        .then(() => {
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
    }
  })

  it("Verify user successfully after 60 seconds wait", () => {
    cy.visit(`/email_verification/${testUser.username}/${testUser.email}`);

    cy.contains("We sent a verification link to").should('be.visible')
    cy.contains(testUser.email).should('be.visible')
    cy.contains("Verify").should('be.visible')
    cy.wait(60_000)

    cy.contains("Resend email to proceed. ").should('be.visible')
    cy.get("button[type='button']").contains("Resend").should('be.visible').click()

    cy.contains("We sent a verification link to").should('be.visible')
    cy.contains(testUser.email).should('be.visible')

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
  })

  it("Verify user successfully and login", () => {
    cy.fetchVerificationToken({
          username: testUser.username,
          email: testUser.email,
        })
      .then((token) => {
        console.log('fetchVerificationToken /api/auth/verification-token response:', token );
        cy.visit(`/email_verification/${testUser.username}/${testUser.email}`);

        cy.contains("We sent a verification link to").should('be.visible')
        cy.contains(testUser.email).should('be.visible')
        cy.contains("Verify").should('be.visible')

        cy.get("input#code").type(token);
        cy.get("button[type='submit']").click();

        cy.url().should('eq', `${Cypress.config().baseUrl}/login`);

        verified = true;
        cy.get('input#username').type(testUser.username);
        cy.get('input[type="password"]').type(testUser.password);
        cy.get('button[type="submit"]').click();
        cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);


      });

  });

  it("Verify user with expired code should fail", () => {
    cy.fetchVerificationToken({
      username: testUser.username,
      email: testUser.email,
    })
      .then((token) => {
        console.log('fetchVerificationToken /api/auth/verification-token response:', token );
        cy.wait(62_000)
        cy.visit(`/email_verification/${testUser.username}/${testUser.email}`);

        cy.get("input#code").type(token);
        cy.get("button[type='submit']").click();


        cy.url().should('eq', `${Cypress.config().baseUrl}/email_verification/${testUser.username}/${testUser.email}`);

        cy.contains("Verification failed. User does not exist or Token is invalid").should('be.visible')



      });

  });

  it("Verify with wrong token should show error", () => {

    cy.fetchVerificationToken({
      username: testUser.username,
      email: testUser.email,
    })
      .then((token) => {
        console.log('fetchVerificationToken /api/auth/verification-token response:', token);
        cy.visit(`/email_verification/${testUser.username}/${testUser.email}`);

        if (token == "000000") {
          token = "111111"
        } else {
          token = "000000"
        }

        cy.contains("We sent a verification link to").should('be.visible')
        cy.contains(testUser.email).should('be.visible')
        cy.contains("Verify").should('be.visible')

        cy.get("input#code").type(token);
        cy.get("button[type='submit']").click();


        cy.url().should('eq', `${Cypress.config().baseUrl}/email_verification/${testUser.username}/${testUser.email}`);

        cy.contains("Verification failed. User does not exist or Token is invalid").should('be.visible')
        cy.visit("/login")
        cy.get('input#username').type(testUser.username);
        cy.get('input[type="password"]').type(testUser.password);
        cy.get('button[type="submit"]').click();
        cy.contains("Email not verified")
        cy.url().should('eq', `${Cypress.config().baseUrl}/login`);


      });
  });

  it ("On change of email address verify user again and not allow the old token", () => {
    cy.fetchVerificationToken({
      username: testUser.username,
      email: testUser.email,
    })
      .then((token) => {
        cy.visit(`/email_verification/${testUser.username}/${testUser.email}`);

        cy.contains("We sent a verification link to").should('be.visible')
        cy.contains(testUser.email).should('be.visible')
        cy.contains("Verify").should('be.visible')

        cy.get("input#code").type(token);
        cy.get("button[type='submit']").click();


        cy.url().should('eq', `${Cypress.config().baseUrl}/login`);

        verified = true

        cy.get('input#username').type(testUser.username);
        cy.get('input[type="password"]').type(testUser.password);
        cy.get('button[type="submit"]').click();
        cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

        cy.get('button[aria-label="Open profile settings"]')
          .should('be.visible')
          .click();

        cy.contains("Profile Details")
        cy.get('input#email').clear().type("newEmailTest@local.test");
        cy.contains('button', 'Save Changes')
          .should('be.visible')
          .click();

        cy.url().should('eq', `${Cypress.config().baseUrl}/email_verification/${testUser.username}/newEmailTest@local.test`);

        cy.get("input#code").type(token);
        cy.get("button[type='submit']").click();


        cy.url().should('eq', `${Cypress.config().baseUrl}/email_verification/${testUser.username}/newEmailTest@local.test`);

        cy.contains("Verification failed. User does not exist or Token is invalid").should('be.visible')
        verified = false
      });
    })

  it.only ("On change of email address verify user again and use new token", () => {
    cy.fetchVerificationToken({
      username: testUser.username,
      email: testUser.email,
    })
      .then((token) => {
        cy.visit(`/email_verification/${testUser.username}/${testUser.email}`);

        cy.contains("We sent a verification link to").should('be.visible')
        cy.contains(testUser.email).should('be.visible')
        cy.contains("Verify").should('be.visible')

        cy.get("input#code").type(token);
        cy.get("button[type='submit']").click();


        cy.url().should('eq', `${Cypress.config().baseUrl}/login`);

        verified = true

        cy.get('input#username').type(testUser.username);
        cy.get('input[type="password"]').type(testUser.password);
        cy.get('button[type="submit"]').click();
        cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

        cy.get('button[aria-label="Open profile settings"]')
          .should('be.visible')
          .click();

        cy.contains("Profile Details")
        cy.get('input#email').clear().type("newEmailTest@local.test");
        cy.contains('button', 'Save Changes')
          .should('be.visible')
          .click();

        cy.url().should('eq', `${Cypress.config().baseUrl}/email_verification/${testUser.username}/newEmailTest@local.test`);

        verified = false
        cy.fetchVerificationToken({
          username: testUser.username,
          email: testUser.email,
        })
          .then((newToken) => {
            cy.get("input#code").type(newToken);
            cy.get("button[type='submit']").click();


            cy.url().should('eq', `${Cypress.config().baseUrl}/login`);

            verified = true
          });
      });
  })

});

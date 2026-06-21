import { environment } from '../../../src/environments/environment';

const describePasswordReset = environment.auth.requireEmailVerification ? describe.skip : describe;

interface TestUser {
  username: string;
  password: string;
  email: string;
  name: string;
  birthdate: string;
}

describePasswordReset("Report account test", () => {
  let reportedUser: TestUser;
  let reportingUser: TestUser;
  let suspended = false;

  const adminUser = {
    username: 'admin',
    password: 'ctrl-test-admin-pw',
  };

  beforeEach(function () {
    const timestamp = Date.now();

    reportedUser = {
      username: `ReportedUser_${timestamp}`,
      password: '$TestUser123',
      email: `reported.user.${timestamp}@local.test`,
      name: 'ReportedUser',
      birthdate: '2000-01-01',
    };

    reportingUser = {
      username: `ReportingUser_${timestamp}`,
      password: '$TestUser123',
      email: `reporting.user.${timestamp}@local.test`,
      name: 'ReportingUser',
      birthdate: '2001-01-01',
    };

    suspended = false;

    cy
      .register(reportedUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);
      });
    cy
      .register(reportingUser)
      .then((registerResponse) => {
        expect(registerResponse.status).to.eq(201);

      });
  });

  afterEach(function () {
    if (!suspended) {
      cy.deleteUser(reportedUser.username, reportedUser.password).then((res) => {
        expect(res.status).to.eq(204);
      });
    }

    cy.deleteUser(reportingUser.username, reportingUser.password).then((res) => {
      expect(res.status).to.eq(204);
    });
  })

  it ("Reporting of account should display reporting flag", () => {
    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile/${reportedUser.username}`);
    cy.get('button[aria-label="Open public profile actions"]')
      .should('be.visible')
      .click();
    cy.contains('.p-menu-item', 'Report').should("be.visible").click();


    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();
    cy.get("#description").should("not.exist");
    cy.reload();
    cy.contains("🚩").should("be.visible").click();
  })

  it ("Reporting of account should display report in admin reporting panel", () => {
    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile/${reportedUser.username}`);
    cy.get('button[aria-label="Open public profile actions"]')
      .should('be.visible')
      .click();
    cy.contains('.p-menu-item', 'Report').should("be.visible").click();


    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();

    cy.visit('/login');
    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.wait(3000)
    cy.get('.nav-admin-item').find('> .p-menubar-item-content a').should('be.visible').click();
    cy.get('#pn_id_4_5_0 > .p-menubar-item-content > .p-ripple').should("be.visible").click();
    cy.contains("Accounts").should("be.visible").click();

    cy.contains("This is the reason for why this is reported").should("be.visible");
    cy.contains(reportedUser.username).should("be.visible")
    cy.contains(reportingUser.username).should("be.visible")
  })

  it ("Reporting of account with dismiss action, should allow account to remain findable", () => {
    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile/${reportedUser.username}`);
    cy.get('button[aria-label="Open public profile actions"]')
      .should('be.visible')
      .click();
    cy.contains('.p-menu-item', 'Report').should("be.visible").click();


    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();

    cy.visit('/login');
    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.wait(3000)
    cy.get('.nav-admin-item').find('> .p-menubar-item-content a').should('be.visible').click();
    cy.get('#pn_id_4_5_0 > .p-menubar-item-content > .p-ripple').should("be.visible").click();
    cy.contains("Accounts").should("be.visible").click();

    cy.contains("This is the reason for why this is reported").should("be.visible");
    cy.contains(reportedUser.username).should("be.visible")
    cy.contains(reportingUser.username).should("be.visible")

    cy.contains("button", "Dismiss").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.visit("/search/users")
    cy.get('input[type="text"]').should("be.visible").type(reportedUser.username);
    cy.contains(reportedUser.username).should("be.visible");
  })

  it ("Reporting of account with suspend action, should not allow account to remain findable", () => {
    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile/${reportedUser.username}`);
    cy.get('button[aria-label="Open public profile actions"]')
      .should('be.visible')
      .click();
    cy.contains('.p-menu-item', 'Report').should("be.visible").click();


    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();

    cy.visit('/login');
    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.wait(3000)
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').should('be.visible').click();
    cy.get('#pn_id_4_5_0 > .p-menubar-item-content > .p-ripple').should("be.visible").click();
    cy.contains("Accounts").should("be.visible").click();

    cy.contains("This is the reason for why this is reported").should("be.visible");
    cy.contains(reportedUser.username).should("be.visible")
    cy.contains(reportingUser.username).should("be.visible")

    cy.contains("button", "Actions").should("be.visible").click();

    cy.get("textarea").invoke('val', 'This post violates our community guidelines and has been removed.')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Suspend Account").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    suspended = true;

    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.visit("/search/users")
    cy.get('input[type="text"]').should("be.visible").type(reportedUser.username);
    cy.contains(reportedUser.username).should("not.exist");
  })

  it ("Reporting of account with suspend action, should prevent login", () => {
    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile/${reportedUser.username}`);
    cy.get('button[aria-label="Open public profile actions"]')
      .should('be.visible')
      .click();
    cy.contains('.p-menu-item', 'Report').should("be.visible").click();


    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();

    cy.visit('/login');
    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.wait(3000)
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').should('be.visible').click();
    cy.get('#pn_id_4_5_0 > .p-menubar-item-content > .p-ripple').should("be.visible").click();
    cy.contains("Accounts").should("be.visible").click();

    cy.contains("This is the reason for why this is reported").should("be.visible");
    cy.contains(reportedUser.username).should("be.visible")
    cy.contains(reportingUser.username).should("be.visible")

    cy.contains("button", "Actions").should("be.visible").click();

    cy.get("textarea").invoke('val', 'This post violates our community guidelines and has been removed.')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Suspend Account").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    suspended = true;

    cy.visit('/login');
    cy.get('input#username').type(reportedUser.username);
    cy.get('input[type="password"]').type(reportedUser.password);
    cy.get('button[type="submit"]').click();

    cy.contains("Invalid username or password").should("be.visible")
  })

})

import { environment } from '../../../src/environments/environment';

const describePasswordReset = environment.auth.requireEmailVerification ? describe.skip : describe;

interface TestUser {
  username: string;
  password: string;
  email: string;
  name: string;
  birthdate: string;
}

describePasswordReset("Report post test", () => {
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

  function setUp() {
    cy.visit('/login');
    cy.get('input#username').type(reportedUser.username);
    cy.get('input[type="password"]').type(reportedUser.password);
    cy.get('button[type="submit"]').click();

    cy.get('button[aria-label="Open profile settings"]')
      .should('be.visible')
      .click();

    cy.contains('button', 'Verify Now')
      .should('be.visible')
      .click();

    cy.contains('button', 'Business Account')
      .should('be.visible')
      .click();

    cy.visit('/login')

    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_1 > .p-menubar-item-content > .p-ripple').click();
    cy.contains(reportedUser.username)
    cy.contains(reportedUser.email)
    cy.contains(reportedUser.name)

    cy.contains("button", "Approve")
      .should("be.visible")
      .click();
  }

  it ("Reporting of post should display reporting flag", () => {
    setUp();
    cy.visit('/login');
    cy.get('input#username').type(reportedUser.username);
    cy.get('input[type="password"]').type(reportedUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);


    cy.visit("/post")
    cy.contains("button", "Weiter").should("be.visible").click();
    cy.get('textarea').should("be.visible").invoke('val', 'This is a test post')
      .trigger('input')
      .trigger('change');

    cy.get('#isPublic').click();

    cy.get('.p-dialog-content').should('be.visible').scrollTo('bottom');
    cy.contains("button", "Create Post").scrollIntoView().should("be.visible").click();

    cy.contains("This is a test post").should("be.visible");


    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)

    cy.contains("This is a test post").should("be.visible"). click();
    cy.get('.post-actions-right > .action-btn').should("be.visible").click();
    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();
    cy.wait(3000)
    cy.reload();
    cy.contains("This is a test post").should("be.visible"). click();
    cy.contains("🚩").should("be.visible").click();
  })

  it ("Reporting of post should be visible in admin reporting tab", () => {
    setUp();
    cy.visit('/login');
    cy.get('input#username').type(reportedUser.username);
    cy.get('input[type="password"]').type(reportedUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);


    cy.visit("/post")
    cy.contains("button", "Weiter").should("be.visible").click();
    cy.get('textarea').should("be.visible").invoke('val', 'This is a test post')
      .trigger('input')
      .trigger('change');

    cy.get('#isPublic').click();

    cy.get('.p-dialog-content').should('be.visible').scrollTo('bottom');
    cy.contains("button", "Create Post").scrollIntoView().should("be.visible").click();

    cy.contains("This is a test post").should("be.visible");


    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)

    cy.contains("This is a test post").should("be.visible"). click();
    cy.get('.post-actions-right > .action-btn').should("be.visible").click();
    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();
    cy.wait(3000)

    cy.visit('/login');
    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.contains("Reports").should("be.visible").click();

    cy.contains("This is a test post").should("be.visible")
    cy.contains("This is the reason for why this is reported").should("be.visible");
    cy.contains(reportedUser.username).should("be.visible")
    cy.contains(reportingUser.username).should("be.visible")
  })

  it ("Reporting of post should should remain visible after dismiss", () => {
    setUp();
    cy.visit('/login');
    cy.get('input#username').type(reportedUser.username);
    cy.get('input[type="password"]').type(reportedUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);


    cy.visit("/post")
    cy.contains("button", "Weiter").should("be.visible").click();
    cy.get('textarea').should("be.visible").invoke('val', 'This is a test post')
      .trigger('input')
      .trigger('change');

    cy.get('#isPublic').click();

    cy.get('.p-dialog-content').should('be.visible').scrollTo('bottom');
    cy.contains("button", "Create Post").scrollIntoView().should("be.visible").click();

    cy.contains("This is a test post").should("be.visible");


    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)

    cy.contains("This is a test post").should("be.visible"). click();
    cy.get('.post-actions-right > .action-btn').should("be.visible").click();
    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();
    cy.wait(3000)

    cy.visit('/login');
    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.contains("Reports").should("be.visible").click();

    cy.contains("button", "Dismiss").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)

    cy.contains("This is a test post").should("be.visible")
  })

  it ("Reporting of post should should not be findable after remove", () => {
    setUp();
    cy.visit('/login');
    cy.get('input#username').type(reportedUser.username);
    cy.get('input[type="password"]').type(reportedUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);


    cy.visit("/post")
    cy.contains("button", "Weiter").should("be.visible").click();
    cy.get('textarea').should("be.visible").invoke('val', 'This is a test post')
      .trigger('input')
      .trigger('change');

    cy.get('#isPublic').click();

    cy.get('.p-dialog-content').should('be.visible').scrollTo('bottom');
    cy.contains("button", "Create Post").scrollIntoView().should("be.visible").click();

    cy.contains("This is a test post").should("be.visible");


    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)

    cy.contains("This is a test post").should("be.visible"). click();
    cy.get('.post-actions-right > .action-btn').should("be.visible").click();
    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();
    cy.wait(3000)

    cy.visit('/login');
    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.contains("Reports").should("be.visible").click();

    cy.contains("button", "Actions").should("be.visible").click();

    cy.get("textarea").invoke('val', 'This post violates our community guidelines and has been removed.')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Take Down Post").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    cy.contains("This is a test post").should("not.exist")

    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)

    cy.contains(reportedUser.username).should("be.visible")
    cy.contains("This is a test post").should("not.exist")

    cy.visit("/search/posts")
    cy.get('input[type="text"]').should("be.visible").type("This is a test");
    cy.contains("button","Search").should("be.visible").click();
    cy.contains(reportedUser.username).should("not.exist");
  })

  it ("Reporting of post user should not be findable after suspend", () => {
    setUp();
    cy.visit('/login');
    cy.get('input#username').type(reportedUser.username);
    cy.get('input[type="password"]').type(reportedUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);


    cy.visit("/post")
    cy.contains("button", "Weiter").should("be.visible").click();
    cy.get('textarea').should("be.visible").invoke('val', 'This is a test post')
      .trigger('input')
      .trigger('change');

    cy.get('#isPublic').click();

    cy.get('.p-dialog-content').should('be.visible').scrollTo('bottom');
    cy.contains("button", "Create Post").scrollIntoView().should("be.visible").click();

    cy.contains("This is a test post").should("be.visible");


    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)

    cy.contains("This is a test post").should("be.visible"). click();
    cy.get('.post-actions-right > .action-btn').should("be.visible").click();
    cy.contains("Bullying or Harassment").should("be.visible").click();
    cy.contains("button", "Continue").scrollIntoView().should("be.visible").click();

    cy.get("#description").should("be.visible")
      .invoke('val', 'This is the reason for why this is reported')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Submit Report").should("be.visible").click();
    cy.wait(3000)

    cy.visit('/login');
    cy.get('input#username').type(adminUser.username);
    cy.get('input[type="password"]').type(adminUser.password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.contains("Reports").should("be.visible").click();

    cy.contains("button", "Actions").should("be.visible").click();

    cy.get("textarea").invoke('val', 'This post violates our community guidelines and has been removed.')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Suspend Account").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    suspended = true;

    cy.contains("This is a test post").should("not.exist")

    cy.visit('/login');
    cy.get('input#username').type(reportingUser.username);
    cy.get('input[type="password"]').type(reportingUser.password);
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);

    cy.visit("/profile/"+ reportedUser.username)

    cy.contains("User not found").should("be.visible")
    cy.contains("This is a test post").should("not.exist")

    cy.visit("/search/posts")
    cy.get('input[type="text"]').should("be.visible").type("This is a test");
    cy.contains("button","Search").should("be.visible").click();
    cy.contains(reportedUser.username).should("not.exist");
  })
});

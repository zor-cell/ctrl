import { environment } from '../../../src/environments/environment';

const describePasswordReset = environment.auth.requireEmailVerification ? describe.skip : describe;

interface TestUser {
  username: string;
  password: string;
  email: string;
  name: string;
  birthdate: string;
}

describePasswordReset('Report source test', () => {
  let reportedUser: TestUser;
  let reportingUser: TestUser;

  const adminUser = {
    username: 'admin',
    password: 'ctrl-test-admin-pw',
  };

  let sourceTitle: string;
  let sourceUrl: string;
  let postCaption: string;

  function login(username: string, password: string) {
    cy.visit('/login');
    cy.get('input#username').type(username);
    cy.get('input[type="password"]').type(password);
    cy.get('button[type="submit"]').click();
    cy.url().should('eq', `${Cypress.config().baseUrl}/profile`);
  }

  function approveBusinessAccount() {
    login(reportedUser.username, reportedUser.password);

    cy.get('button[aria-label="Open profile settings"]')
      .should('be.visible')
      .click();

    cy.contains('button', 'Verify Now')
      .should('be.visible')
      .click();

    cy.contains('button', 'Business Account')
      .should('be.visible')
      .click();

    login(adminUser.username, adminUser.password);

    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_1 > .p-menubar-item-content > .p-ripple').click();
    cy.contains(reportedUser.username);
    cy.contains(reportedUser.email);
    cy.contains(reportedUser.name);

    cy.contains('button', 'Approve')
      .should('be.visible')
      .click();
  }

  function createPostWithSource() {
    login(reportedUser.username, reportedUser.password);

    cy.visit('/post');
    cy.contains('button', 'Weiter').should('be.visible').click();

    cy.get('textarea')
      .should('be.visible')
      .invoke('val', postCaption)
      .trigger('input')
      .trigger('change');

    cy.get('#isPublic').click();

    cy.get('.p-dialog-content').should('be.visible').scrollTo('bottom');
    cy.get('#title-0').should('be.visible').type(sourceTitle);
    cy.get('#url-0').should('be.visible').type(sourceUrl);

    cy.contains('button', 'Create Post').scrollIntoView().should('be.visible').click();

    cy.contains(postCaption).should('be.visible');
  }

  function reportCreatedSource() {
    login(reportingUser.username, reportingUser.password);
    cy.visit(`/profile/${reportedUser.username}`);

    cy.contains(postCaption).should('be.visible').click();
    cy.contains('button', 'Sources').should('be.visible').click();
    cy.contains(sourceTitle).should('be.visible');

    cy.get('button[aria-label="Report source"]').should('be.visible').click();
    cy.contains('Bullying or Harassment').should('be.visible').click();
    cy.contains('button', 'Continue').scrollIntoView().should('be.visible').click();

    cy.get('#description')
      .should('be.visible')
      .invoke('val', 'This source is misleading and should be reviewed')
      .trigger('input')
      .trigger('change');

    cy.contains('button', 'Submit Report').should('be.visible').click();
  }

  let suspended = false

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

    sourceTitle = `Source title ${timestamp}`;
    sourceUrl = `https://example.com/source-${timestamp}`;
    postCaption = `Post with source ${timestamp}`;

    cy.register(reportedUser).then((registerResponse) => {
      expect(registerResponse.status).to.eq(201);
    });

    cy.register(reportingUser).then((registerResponse) => {
      expect(registerResponse.status).to.eq(201);
    });

    approveBusinessAccount();
  });

  afterEach(function () {
    if (!suspended){
      cy.deleteUser(reportedUser.username, reportedUser.password).then((res) => {
        expect(res.status).to.eq(204);
      });

    }

    cy.deleteUser(reportingUser.username, reportingUser.password).then((res) => {
      expect(res.status).to.eq(204);
    });
  });

  it('Reporting of source should display reporting flag and keep source visible', () => {
    createPostWithSource();
    reportCreatedSource();

    cy.reload();
    cy.contains(postCaption).should('be.visible').click();
    cy.contains('button', 'Sources').should('be.visible').click();
    cy.contains(sourceTitle).should('be.visible');
    cy.contains('🚩').should('be.visible').click();
  });

  it('Reporting of source should be visible in admin reporting tab', () => {
    createPostWithSource();
    reportCreatedSource();

    login(adminUser.username, adminUser.password);
    cy.wait(3000)
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_0 > .p-menubar-item-content > .p-ripple').should('be.visible').click();
    cy.contains('Sources').should('be.visible').click();

    cy.contains(sourceTitle).should('be.visible');
    cy.contains(sourceUrl).should('be.visible');
    cy.contains(reportedUser.username).should('be.visible');
    cy.contains(reportingUser.username).should('be.visible');
  });

  it('Reporting of source should be visible if report is dismissed', () => {
    createPostWithSource();
    reportCreatedSource();

    login(adminUser.username, adminUser.password);
    cy.wait(3000)
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_0 > .p-menubar-item-content > .p-ripple').should('be.visible').click();
    cy.contains('Sources').should('be.visible').click();

    cy.contains(sourceTitle).should('be.visible');
    cy.contains(sourceUrl).should('be.visible');
    cy.contains(reportedUser.username).should('be.visible');
    cy.contains(reportingUser.username).should('be.visible');

    cy.contains("button", "Dismiss").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    login(reportingUser.username, reportingUser.password);
    cy.visit(`/profile/${reportedUser.username}`);

    cy.contains(postCaption).should('be.visible').click();
    cy.contains('button', 'Sources').should('be.visible').click();
    cy.contains(sourceTitle).should('be.visible');
  });

  it('Reporting of source should not be visible if report is taking down source', () => {
    createPostWithSource();
    reportCreatedSource();

    login(adminUser.username, adminUser.password);
    cy.wait(3000)
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_0 > .p-menubar-item-content > .p-ripple').should('be.visible').click();
    cy.contains('Sources').should('be.visible').click();

    cy.contains(sourceTitle).should('be.visible');
    cy.contains(sourceUrl).should('be.visible');
    cy.contains(reportedUser.username).should('be.visible');
    cy.contains(reportingUser.username).should('be.visible');

    cy.contains("button", "Actions").should("be.visible").click();

    cy.get("textarea").invoke('val', 'This post violates our community guidelines and has been removed.')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Take Down Source").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    login(reportingUser.username, reportingUser.password);
    cy.visit(`/profile/${reportedUser.username}`);

    cy.contains(postCaption).should('be.visible').click();
    cy.contains('button', 'Sources').should('not.exist');
    cy.contains(sourceTitle).should('not.exist');
  });

  it('Reporting of source should not be visible if report is suspending user', () => {
    createPostWithSource();
    reportCreatedSource();

    login(adminUser.username, adminUser.password)
    cy.wait(3000)
    cy.get('#pn_id_4_5 > :nth-child(1) > .p-ripple').click();
    cy.get('#pn_id_4_5_0 > .p-menubar-item-content > .p-ripple').should('be.visible').click();
    cy.contains('Sources').should('be.visible').click();

    cy.contains(sourceTitle).should('be.visible');
    cy.contains(sourceUrl).should('be.visible');
    cy.contains(reportedUser.username).should('be.visible');
    cy.contains(reportingUser.username).should('be.visible');

    cy.contains("button", "Actions").should("be.visible").click();

    cy.get("textarea").invoke('val', 'This post violates our community guidelines and has been removed.')
      .trigger('input')
      .trigger('change');
    cy.contains("button", "Suspend Account").should("be.visible").click();
    cy.contains("button", "Yes").should("be.visible").click();

    suspended = true

    login(reportingUser.username, reportingUser.password);
    cy.visit(`/profile/${reportedUser.username}`);

    cy.contains("User not found").should("be.visible")
  });
});


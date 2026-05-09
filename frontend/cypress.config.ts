import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:4200',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    supportFile: 'cypress/support/e2e.ts',
    video: false,
    screenshotOnRunFailure: true,
    allowCypressEnv: false,
    env: {
      apiUrl: 'http://localhost:8080',
      TEST_USER: 'testuser@example.com',
      TEST_PASSWORD: 'testpassword123',
      TEST_ADMIN: 'admin@example.com',
      TEST_ADMIN_PASSWORD: 'adminpassword123',
    },
  },
});

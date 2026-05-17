export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080/api',
  idAustria: {
    clientId: 'http://localhost:8080',
    redirectUri: 'http://localhost:80/verification-callback',
    authUrl: 'http://localhost:8081/realms/id-austria-mock/protocol/openid-connect/auth',
    scope: 'openid profile'
  },
  auth: {
    requireEmailVerification: true
  }
};

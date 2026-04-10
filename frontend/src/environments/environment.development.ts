export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  idAustria: {
    clientId: 'http://localhost:8080',
    redirectUri: 'http://localhost:4200/verification-callback',
    authUrl: 'http://localhost:8081/realms/id-austria-mock/protocol/openid-connect/auth',
    scope: 'openid profile'
  }
};

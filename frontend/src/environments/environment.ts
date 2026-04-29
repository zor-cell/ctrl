export const environment = {
  production: true,
  apiUrl: 'http://backend-service/api',
  idAustria: {
    clientId: 'http://backend-service:8080',
    redirectUri: 'http://backend-service/verification-callback',
    authUrl: 'http://verification-service/realms/id-austria-mock/protocol/openid-connect/auth',
    scope: 'openid profile'
  },
  auth: {
    requireEmailVerification: true
  }
};

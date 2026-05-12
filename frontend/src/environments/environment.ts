export const environment = {
  production: true,
  apiUrl: 'https://ctrl.nilpferdshow.at/api',
  idAustria: {
    clientId: 'https://ctrl.nilpferdshow.at',
    redirectUri: 'https://ctrl.nilpferdshow.at/verification-callback',
    authUrl: 'http://verification-service/realms/id-austria-mock/protocol/openid-connect/auth',
    scope: 'openid profile'
  },
  auth: {
    requireEmailVerification: true
  }
};

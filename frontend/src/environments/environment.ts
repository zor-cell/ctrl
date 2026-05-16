export const environment = {
  production: true,
  apiUrl: 'https://www.ctrl-ase.dev/api',
  idAustria: {
    clientId: 'https://www.ctrl-ase.dev',
    redirectUri: 'https://www.ctrl-ase.dev/verification-callback',
    authUrl: 'https://www.ctrl-ase.dev/id-austria-mock/protocol/openid-connect/auth',
    scope: 'openid profile'
  },
  auth: {
    requireEmailVerification: false
  }
};

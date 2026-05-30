export const environment = {
  production: true,
  apiUrl: 'https://www.ctrl-ase.dev/api',
  wsUrl: 'https://www.ctrl-ase.dev/ws',
  idAustria: {
    clientId: 'https://www.ctrl-ase.dev',
    redirectUri: 'https://www.ctrl-ase.dev/verification-callback',
    authUrl: 'https://www.ctrl-ase.dev/realms/id-austria-mock/protocol/openid-connect/auth',
    scope: 'openid profile'
  },
  auth: {
    requireEmailVerification: true
  },
  heartbeat: {
    interval: 60000 // 1 min in ms
  }
};

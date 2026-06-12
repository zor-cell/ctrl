import { describe, expect, it } from 'vitest';

import { isPublicAuthRoute } from './auth-routes';

describe('isPublicAuthRoute', () => {
  it('treats root and login routes as public auth routes', () => {
    expect(isPublicAuthRoute('')).toBe(true);
    expect(isPublicAuthRoute('/')).toBe(true);
    expect(isPublicAuthRoute('/login')).toBe(true);
    expect(isPublicAuthRoute('/login?redirect=%2Ffeed')).toBe(true);
  });

  it('does not treat authenticated app routes as public', () => {
    expect(isPublicAuthRoute('/feed')).toBe(false);
    expect(isPublicAuthRoute('/chat')).toBe(false);
    expect(isPublicAuthRoute('/profile')).toBe(false);
  });
});

export function isPublicAuthRoute(url: string): boolean {
  const path = url.split('?')[0];
  return (
    path === '/login' ||
    path === '/register' ||
    path === '/forgot-password' ||
    path.startsWith('/reset-password') ||
    path.startsWith('/email_verification')
  );
}

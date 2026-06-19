export function usernameInitial(username: string | null | undefined): string {
  const trimmed = username?.trim();
  if (!trimmed) {
    return '?';
  }

  if (trimmed.startsWith('.') || trimmed.startsWith('_')) {
    const match = trimmed.match(/[a-zA-Z0-9]/);
    return match ? match[0].toUpperCase() : '?';
  }

  return trimmed.charAt(0).toUpperCase() || '?';
}

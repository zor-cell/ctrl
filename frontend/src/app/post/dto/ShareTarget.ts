export interface ShareTarget {
  id: string;
  kind: 'direct' | 'group';
  chatId: string | null;
  username: string | null;
  title: string;
  subtitle: string;
  avatarRef: string | null;
  interactionCount: number;
  lastActivityTime: number;
}

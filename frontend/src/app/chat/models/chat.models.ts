export interface ChatParticipant {
  id: string;
  username: string;
  name: string | null;
  profilePicture: string | null;
  deleted: boolean;
}

export interface MessageReaction {
  emoji: string;
  count: number;
  reactedByCurrentUser: boolean;
  users: string[];
}

export interface SharedPost {
  sharedId: string;
  postId: string;
  username: string;
  profileUserId: string | null;
  profilePicture: string | null;
  caption: string | null;
  mediaRef: string | null;
  mediaKind: 'image' | 'video' | null;
  sharedAt: string;
  canView: boolean;
  publicPost: boolean;
  profileUsername: string | null;
  unavailableReason: 'PRIVATE' | 'REMOVED' | null;
}

export type SystemEventType = 'MEMBER_ADDED' | 'MEMBER_REMOVED' | 'MEMBER_LEFT';

export interface ChatMessage {
  messageId: string;
  chatId: string;
  sender: ChatParticipant;
  content: string;
  date: string;
  own: boolean;
  system: boolean;
  systemEvent: SystemEventType | null;
  systemTargets: ChatParticipant[];
  reactions: MessageReaction[];
  sharedPost: SharedPost | null;
}

export interface Chat {
  chatId: string;
  name: string;
  groupImage: string | null;
  participants: ChatParticipant[];
  lastMessage: ChatMessage | null;
  createdAt: string;
  groupChat: boolean;
  ownerId: string | null;
  ownerIds: string[];
  groupOwner: ChatParticipant | null;
  owner: boolean;
  personalMessageCount: number;
  unreadCount: number;
}

export interface CreateChatRequest {
  name?: string | null;
  usernames: string[];
}

export interface SendMessageRequest {
  content: string;
  postIdToShare?: string | null;
}

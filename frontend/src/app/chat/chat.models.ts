export interface ChatParticipant {
  id: string;
  username: string;
  name: string | null;
  profilePicture: string | null;
}

export interface MessageReaction {
  emoji: string;
  count: number;
  reactedByCurrentUser: boolean;
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
  groupOwner: ChatParticipant | null;
  owner: boolean;
  unreadCount: number;
}

export interface CreateChatRequest {
  name?: string | null;
  usernames: string[];
}

export interface SendMessageRequest {
  content: string;
}

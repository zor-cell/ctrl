export interface ChatParticipant {
  id: string;
  username: string;
  name: string | null;
  profilePicture: string | null;
}

export interface ChatMessage {
  messageId: string;
  chatId: string;
  sender: ChatParticipant;
  content: string;
  date: string;
  own: boolean;
}

export interface Chat {
  chatId: string;
  name: string;
  participants: ChatParticipant[];
  lastMessage: ChatMessage | null;
  createdAt: string;
  groupChat: boolean;
  owner: boolean;
}

export interface CreateChatRequest {
  name?: string | null;
  usernames: string[];
}

export interface SendMessageRequest {
  content: string;
}

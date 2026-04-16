export interface Friend {
  id: string;
  username: string;
  name: string;
  profilePicture: string | null;
}

export interface FollowedUser {
  id: string;
  username: string;
  profilePicture: string | null;
}

export interface FriendRequest {
  id: string;
  username: string;
  profilePicture: string | null;
  isIncoming: boolean;
  createdAt: string;
}

export interface UserSearchResult {
  id: string;
  username: string;
  name: string | null;
  profilePicture: string | null;
  isVerified: boolean;
  relationshipStatus: 'NONE' | 'FOLLOWING' | 'FRIEND' | 'PENDING_REQUEST_SENT' | 'PENDING_REQUEST_INCOMING';
}

export interface SendFriendRequest {
  toUsername: string;
}

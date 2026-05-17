export interface Friend {
  id: string;
  username: string;
  name: string;
  profilePicture: string | null;
}

export interface Followed {
  id: string;
  username: string;
  profilePicture: string | null;
}

/**
 * FriendRequest response from backend.
 * The username field always contains the "other" person's username:
 * - For incoming requests: the sender's username
 * - For sent requests: the receiver's username
 */
export interface FriendRequest {
  username: string;
  profilePicture: string | null;
  isIncoming: boolean;
  createdAt: string;
}

export interface RelationshipStatusItem {
  status: 'FRIEND' | 'FOLLOWED' | 'PENDING_REQUEST_SENT' | 'PENDING_REQUEST_INCOMING';
}

export interface UserSearchResult {
  id: string;
  username: string;
  name: string | null;
  profilePicture: string | null;
  isVerified: boolean;
  relationshipStatuses: RelationshipStatusItem[];
}

export interface SendFriendRequest {
  toUsername: string;
}

/**
 * Action enum for PATCH /friend-requests/{username} endpoint
 */
export type FriendRequestAction = 'ACCEPT' | 'DECLINE';

/**
 * DTO for PATCH /friend-requests/{username}
 */
export interface FriendRequestActionDto {
  action: FriendRequestAction;
}

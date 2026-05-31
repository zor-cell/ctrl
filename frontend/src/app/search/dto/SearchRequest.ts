export interface PostSearchRequest {
  query?: string;
  topicIds?: string[];
  candidatePoolSize?: number;
  page: number;
  postsPerPage: number;
}

export interface UserSearchRequest {
  query: string;
  excludeFriends: boolean;
  excludeFollowed: boolean;
  verifiedOnly: boolean;
}

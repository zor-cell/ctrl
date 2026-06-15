export interface PostSource {
  sourceId: string;
  url: string;
  upvotes: number;
  downvotes: number;
  userVote: 'up' | 'down' | null;
  wasReportedByUser: boolean;
}

export interface PostSource {
  sourceId: string;
  url: string;
  upvotes: number;
  downvotes: number;
  userVote: 'up' | 'down' | null;
  wasReportedByUser: boolean;
  createdAt?:         string;
  journal?:           string | null;
  author?:            string | null;
  title?:             string | null;
  date?:              string | null;
}

export interface SessionConfigUpdateResponse {
  timeLimitMinutes: number;
  postLimit: number;
  allowSnooze: boolean;
  appliedImmediately: boolean;
}
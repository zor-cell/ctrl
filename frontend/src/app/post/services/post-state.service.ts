import { Injectable, signal } from '@angular/core';

export interface PostVoteState {
  userVote: 'up' | 'down' | null;
  sourceVote: 'up' | 'down' | null;
}

export interface PostCommentState {
  commentCount: number;
}

/**
 * Lightweight shared store that keeps per-post vote and comment-count state
 * in sync between FeedComponent and PostDetailViewComponent.
 *
 * Both components inject this service (it is root-provided, so it is a
 * singleton for the entire app) and read/write through it instead of keeping
 * their own isolated copies of that state.
 */
@Injectable({ providedIn: 'root' })
export class PostStateService {

  // ─── Internal maps (postId → state) ──────────────────────────────────────

  private readonly _votes    = signal<Record<string, PostVoteState>>({});
  private readonly _comments = signal<Record<string, number>>({});

  // ─── Vote API ─────────────────────────────────────────────────────────────

  /** Returns the current vote state for a post (never undefined). */
  getVote(postId: string): PostVoteState {
    return this._votes()[postId] ?? { userVote: null, sourceVote: null };
  }

  /** Seed initial vote state (called once when the post is first loaded). */
  initVote(postId: string, userVote: 'up' | 'down' | null, sourceVote: 'up' | 'down' | null): void {
    // Only write if not already tracked so we don't overwrite a fresher value.
    if (this._votes()[postId]) return;
    this._votes.update(m => ({ ...m, [postId]: { userVote, sourceVote } }));
  }

  setUserVote(postId: string, userVote: 'up' | 'down' | null): void {
    this._votes.update(m => ({
      ...m,
      [postId]: { ...(m[postId] ?? { sourceVote: null }), userVote },
    }));
  }

  setSourceVote(postId: string, sourceVote: 'up' | 'down' | null): void {
    this._votes.update(m => ({
      ...m,
      [postId]: { ...(m[postId] ?? { userVote: null }), sourceVote },
    }));
  }

  // ─── Comment-count API ───────────────────────────────────────────────────

  /** Returns the tracked comment count, or `undefined` if not yet seeded. */
  getCommentCount(postId: string): number | undefined {
    return this._comments()[postId];
  }

  /** Seed the initial count (e.g. from the feed payload). */
  initCommentCount(postId: string, count: number): void {
    if (this._comments()[postId] !== undefined) return;
    this._comments.update(m => ({ ...m, [postId]: count }));
  }

  /** Overwrite with the authoritative count (e.g. after fetching detail). */
  setCommentCount(postId: string, count: number): void {
    this._comments.update(m => ({ ...m, [postId]: count }));
  }

  /** Increment by 1 when the user posts a new comment. */
  incrementCommentCount(postId: string): void {
    this._comments.update(m => ({ ...m, [postId]: (m[postId] ?? 0) + 1 }));
  }

  /** Read-only snapshot of the whole comment map (used to derive signals). */
  get commentMap() { return this._comments.asReadonly(); }

  /** Read-only snapshot of the whole vote map. */
  get voteMap()    { return this._votes.asReadonly(); }
}

import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { environment } from '../../environments/environment';
import { CreatePostRequest } from './dto/CreatePostRequest';
import { PostResponse } from './dto/PostResponse';
import { TopicResponse } from './dto/TopicResponse';
import {firstValueFrom} from 'rxjs';

export type VoteType = 'up' | 'down';

@Injectable({
  providedIn: 'root',
})
export class PostService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/posts`;
  private readonly topicUri = `${environment.apiUrl}/topics`;
  private readonly sourcesUri = `${environment.apiUrl}/sources`;

  /**
   * Sends a request to create a new post.
   *
   * POST /posts
   *
   * @param request - The payload containing all data for the new post
   * @returns An observable that emits the created {@link PostResponse}
   */
  createPost(request: CreatePostRequest) {
    return this.httpClient.post<PostResponse>(this.baseUri, request);
  }

  /**
   * Fetches all available topics from the backend.
   *
   * GET /topics
   *
   * @returns An observable that emits an array of {@link TopicResponse} objects
   */
  getTopics() {
    return this.httpClient.get<TopicResponse[]>(this.topicUri);
  }

  /**
   * Sends an upvote or downvote for a post to the backend.
   * Sending the same vote type again toggles (removes) the existing vote.
   *
   * PUT /posts/{postId}/vote
   * Body: { voteType: 'up' | 'down' }
   *
   * @param postId - The unique identifier of the post to vote on
   * @param voteType - The vote direction: 'up' for upvote, 'down' for downvote
   * @returns A promise resolving to the updated vote counts and the current user's vote state
   */
  async votePost(
    postId: string,
    voteType: VoteType
  ): Promise<{ upvotes: number; downvotes: number; userVote: 'up' | 'down' | null }> {
    return firstValueFrom(
      this.httpClient.put<{ upvotes: number; downvotes: number; userVote: 'up' | 'down' | null }>(
        `${this.baseUri}/${postId}/vote`,
        { voteType }
      )
    );
  }

  /**
   * Sends an upvote or downvote for a post's source to the backend.
   * Sending the same vote type again toggles (removes) the existing vote.
   *
   * PUT /sources/{sourceId}/vote
   * Body: { voteType: 'up' | 'down' }
   *
   * @param sourceId - The unique identifier of the source to vote on
   * @param voteType - The vote direction: 'up' for upvote, 'down' for downvote
   * @returns A promise resolving to the updated vote counts and the current user's vote state
   */
  async voteSource(
    sourceId: string,
    voteType: VoteType
  ): Promise<{ upvotes: number; downvotes: number; userVote: 'up' | 'down' | null }> {
    return firstValueFrom(
      this.httpClient.put<{ upvotes: number; downvotes: number; userVote: 'up' | 'down' | null }>(
        `${this.sourcesUri}/${sourceId}/vote`,
        { voteType }
      )
    );
  }

}

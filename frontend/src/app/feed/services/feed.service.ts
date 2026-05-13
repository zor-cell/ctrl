//TODO: warning should be fixed!
/* eslint-disable @typescript-eslint/no-explicit-any */

import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Post } from '../dto/Post';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FeedService {
  private readonly httpClient = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/feed`;

  /**
   * Fetches a paginated list of posts for the current user's feed.
   *
   * GET /feed?page={page}&postsPerPage={postsPerPage}
   *
   * @param page - Zero-based page index (default: 0)
   * @param postsPerPage - Number of posts per page (default: 20)
   * @returns A promise resolving to an array of mapped {@link Post} objects
   */

  async getFeed(page = 0, postsPerPage = 20): Promise<Post[]> {
    const response = await firstValueFrom(
      this.httpClient.get<any[]>(`${this.baseUri}?page=${page}&postsPerPage=${postsPerPage}`)
    );
    return response.map(item => this.mapToPost(item));
  }

  /**
   *
   * Maps a raw API response object to a typed {@link Post} model.
   * Applies fallback values for optional fields that may be absent in the response.
   *
   * @param item - Raw response object from the feed API
   * @returns A fully structured {@link Post} object
   */
  private mapToPost(item: any): Post {
    return {
      postId: item.postId,
      userId: item.userId ?? item.username,
      username: item.username,
      userProfilePic: item.userProfilePic ?? '',
      createdAt: item.createdAt,
      caption: item.caption ?? '',
      isPublic: item.isPublic,
      whyExplanation: this.generateScoreExplanation(item.explanation),
      likes: 0,
      upvotes: item.upvotes ?? 0,
      downvotes: item.downvotes ?? 0,
      userVote: item.userVote ?? null,
      labelIds: item.labelIds?.map((l: any) => [l.name, l.explanation]) ?? [],
      topicIds: item.topicIds ?? [],
      videoImageRef: item.mediaRef ? [item.mediaRef] : [],
      sourceIds: item.sourceIds ?? [],
      comments:
        item.comments?.map((c: any) => ({
          commentId: c.commentId,
          userId: c.userId,
          username: c.username,
          userProfilePic: c.userProfilePic ?? '',
          createdAt: c.createdAt,
          content: c.content,
          parentCommentId: c.parentCommentId ?? null,
        })) ?? [],
    };
  }

  private generateScoreExplanation(score: PostScore): string {
    const reasons: string[] = [];

    const THRESHOLD = 0.1;

    if (score.scoreRecency > THRESHOLD) {
      reasons.push(`- recently published (${score.scoreRecency.toFixed(2)})`);
    }
    if (score.scoreTopic > THRESHOLD) {
      reasons.push(`- suits your selected topics (${score.scoreTopic.toFixed(2)})`);
    }
    if (score.scoreInteraction > THRESHOLD) {
      reasons.push(`- is similar to posts you liked, commented, watched for a longer period of time or shared (${score.scoreInteraction.toFixed(2)})`);
    }
    if (score.scoreTrending > THRESHOLD) {
      reasons.push(`- is trending (${score.scoreTrending.toFixed(2)})`);
    }
    if (score.scoreSource > THRESHOLD) {
      reasons.push(`- has a reliable source (${score.scoreSource.toFixed(2)})`);
    }
    if (score.scoreExploration > THRESHOLD) {
      reasons.push(`- from someone you don't follow: your exploration slider boosted this post (${score.scoreExploration.toFixed(2)})`);
    }

    if (reasons.length === 0) {
      return 'This post was chosen at random.';
    }

    return `This post is displayed because it:\n\n${reasons.join('\n')} \n\n The total socre is: ` + score.totalScore.toFixed(2);
  }

}

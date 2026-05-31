import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserSearchResult } from '../../user/dto/Relationship';
import { PostSearchRequest, UserSearchRequest } from '../dto/SearchRequest';
import { PostPreview } from '../dto/PostPreview';

interface CachedPostSearch {
  request: PostSearchRequest;
  results: Observable<PostPreview[]>;
  timestamp: number;
  scrollY: number;
}

@Injectable({
  providedIn: 'root',
})
export class SearchService {
  private readonly baseUri: string = environment.apiUrl + '/search';
  private http = inject(HttpClient);

  // Cache for post search results (5 minute expiration)
  private readonly CACHE_EXPIRATION_MS = 5 * 60 * 1000; // 5 minutes
  private postSearchCache: CachedPostSearch | null = null;

  /**
   * Searches for posts based on query embedding similarity with optional topic filtering.
   * Uses POST with request body to keep URLs clean.
   * Caches results for 5 minutes to avoid redundant API calls.
   */
  searchPosts(request: PostSearchRequest): Observable<PostPreview[]> {
    // Check if we have valid cached results
    const cached = this.getCachedPostSearch(request);
    if (cached) {
      return cached.results;
    }

    const result = this.http.post<PostPreview[]>(`${this.baseUri}/posts`, request);
    this.cachePostSearchResults(request, result);
    return result;
  }

  /**
   * Cache post search results for later retrieval.
   * Call this after receiving results from the API.
   */
  cachePostSearchResults(request: PostSearchRequest, results: Observable<PostPreview[]>): void {
    this.postSearchCache = {
      request: { ...request },
      results: results,
      timestamp: Date.now(),
      scrollY: 0,
    };
  }

  /**
   * Get cached post search results if they match the request and are not expired.
   * Returns null if no valid cache exists.
   */
  getCachedPostSearch(request: PostSearchRequest): CachedPostSearch | null {
    if (!this.postSearchCache) {
      return null;
    }

    // Check if cache is expired
    const age = Date.now() - this.postSearchCache.timestamp;
    if (age > this.CACHE_EXPIRATION_MS) {
      this.postSearchCache = null;
      return null;
    }

    // Check if request matches cached request
    if (this.isSamePostSearchRequest(request, this.postSearchCache.request)) {
      return this.postSearchCache;
    }

    return null;
  }

  /**
   * Clear the post search cache.
   */
  clearPostSearchCache(): void {
    this.postSearchCache = null;
  }

  /**
   * Save scroll position for the current cached search.
   */
  saveScrollPosition(scrollY: number): void {
    if (this.postSearchCache) {
      this.postSearchCache.scrollY = scrollY;
    }
  }

  /**
   * Get the saved scroll position from cache.
   */
  getSavedScrollPosition(): number {
    return this.postSearchCache?.scrollY || 0;
  }

  /**
   * Check if two post search requests are the same (same query and topics).
   */
  private isSamePostSearchRequest(a: PostSearchRequest, b: PostSearchRequest): boolean {
    const sameQuery = (a.query || '') === (b.query || '');
    const sameTopics = this.areArraysEqual(
      (a.topicIds || []).sort(),
      (b.topicIds || []).sort()
    );
    return sameQuery && sameTopics;
  }

  /**
   * Check if two arrays are equal.
   */
  private areArraysEqual(a: string[], b: string[]): boolean {
    if (a.length !== b.length) return false;
    return a.every((val, index) => val === b[index]);
  }

  /**
   * Searches for users based on username matching with optional relationship filters.
   * Uses POST with request body to keep URLs clean.
   */
  searchUsers(request: UserSearchRequest): Observable<UserSearchResult[]> {
    return this.http.post<UserSearchResult[]>(`${this.baseUri}/users`, request);
  }
}

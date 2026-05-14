import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Post } from '../../feed/dto/Post';
import { UserSearchResult } from '../../user/dto/Relationship';
import { PostSearchRequest, UserSearchRequest } from '../dto/SearchRequest';

@Injectable({
  providedIn: 'root',
})
export class SearchService {
  private readonly baseUri: string = environment.apiUrl + '/search';
  private http = inject(HttpClient);

  /**
   * Searches for posts based on query embedding similarity with optional topic filtering.
   * Uses POST with request body to keep URLs clean.
   */
  searchPosts(request: PostSearchRequest): Observable<Post[]> {
    return this.http.post<Post[]>(`${this.baseUri}/posts`, request);
  }

  /**
   * Searches for users based on username matching with optional relationship filters.
   * Uses POST with request body to keep URLs clean.
   */
  searchUsers(request: UserSearchRequest): Observable<UserSearchResult[]> {
    return this.http.post<UserSearchResult[]>(`${this.baseUri}/users`, request);
  }
}

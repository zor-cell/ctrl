import { Injectable, inject, signal, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, forkJoin, map, merge, Observable, of, Subject, switchMap, takeUntil, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Friend, Followed, FriendRequest, SendFriendRequest, UserSearchResult } from './dto/Relationship';
import { FriendRequestEventBusService, FriendRequestActionEvent } from './friend-request-event-bus.service';
import { MediaCacheService } from '../core/services/media-cache.service';
import { SILENT_ERROR_CONTEXT } from '../core/interceptors';

@Injectable({
  providedIn: 'root',
})
export class RelationshipService implements OnDestroy {
  private readonly baseUri: string = environment.apiUrl + '/users/me';
  private readonly searchUri: string = environment.apiUrl + '/users';
  private http = inject(HttpClient);
  private eventBus = inject(FriendRequestEventBusService);
  private mediaCacheService = inject(MediaCacheService);

  private _friends = signal<Friend[]>([]);
  private _followed = signal<Followed[]>([]);
  private _incomingRequests = signal<FriendRequest[]>([]);
  private _sentRequests = signal<FriendRequest[]>([]);
  private _pendingRequestCount = signal<number>(0);
  private _searchResults = signal<UserSearchResult[]>([]);

  public friends = this._friends.asReadonly();
  public followed = this._followed.asReadonly();
  public incomingRequests = this._incomingRequests.asReadonly();
  public sentRequests = this._sentRequests.asReadonly();
  public pendingRequestCount = this._pendingRequestCount.asReadonly();
  public searchResults = this._searchResults.asReadonly();

  private _destroy$ = new Subject<void>();
  private _consolidatedRefresh$ = new Subject<void>();

  constructor() {
    merge(
      this.eventBus.refreshRequests$,
      this._consolidatedRefresh$
    )
      .pipe(
        takeUntil(this._destroy$),
        switchMap(() => this.performConsolidatedRefresh())
      )
      .subscribe();

    this.eventBus.actionEvents$
      .pipe(takeUntil(this._destroy$))
      .subscribe((event) => this.handleActionEvent(event));
  }

  private handleActionEvent(event: FriendRequestActionEvent): void {
    switch (event.type) {
      case 'accepted':
        if (event.username) {
          this.removeIncomingRequest(event.username);
          this.markSearchResultAsFriend(event.username);
        }
        forkJoin([
          this.safeRefresh(this.loadPendingRequestCount()),
          this.safeRefresh(this.loadFriends())
        ]).pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'declined':
        if (event.username) {
          this.removeIncomingRequest(event.username);
        }
        this.safeRefresh(this.loadPendingRequestCount()).pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'cancelled':
        this.safeRefresh(this.loadSentFriendRequests()).pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'sent':
        this.safeRefresh(this.loadSentFriendRequests()).pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'followed':
        this.safeRefresh(this.loadFollowed()).pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'unfriend':
        this.safeRefresh(this.loadFriends()).pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'unfollowed':
        forkJoin([
          this.safeRefresh(this.loadFollowed()),
          this.safeRefresh(this.loadFriends())
        ]).pipe(takeUntil(this._destroy$)).subscribe();
        break;
    }
  }

  private performConsolidatedRefresh() {
    this.eventBus.setLoading(true);

    return merge(
      this.safeRefresh(this.loadIncomingFriendRequests()),
      this.safeRefresh(this.loadSentFriendRequests()),
      this.safeRefresh(this.loadPendingRequestCount())
    ).pipe(
      tap({
        complete: () => {
          this.eventBus.setLoading(false);
          this.eventBus.updateState({
            incoming: this._incomingRequests(),
            sent: this._sentRequests(),
            pendingCount: this._pendingRequestCount(),
          });
        },
      })
    );
  }

  private removeIncomingRequest(username: string): void {
    const incoming = this._incomingRequests()
      .filter(request => request.username.toLowerCase() !== username.toLowerCase());
    const pendingCount = Math.max(this._pendingRequestCount() - 1, 0);
    this._incomingRequests.set(incoming);
    this._pendingRequestCount.set(pendingCount);
    this.eventBus.updateState({ incoming, pendingCount });
  }

  markSearchResultAsFriend(username: string): void {
    this._searchResults.update(results =>
      results.map(result => {
        if (result.username.toLowerCase() !== username.toLowerCase()) {
          return result;
        }

        return {
          ...result,
          relationshipStatuses: [
            ...result.relationshipStatuses.filter(item =>
              item.status !== 'PENDING_REQUEST_INCOMING'
              && item.status !== 'PENDING_REQUEST_SENT'
            ),
            { status: 'FRIEND' as const },
          ],
        };
      })
    );
  }

  loadFriends(search?: string) {
    let params = new HttpParams();
    if (search) {
      params = params.set('q', search);
    }
    return this.http.get<Friend[]>(`${this.baseUri}/friends`, { params }).pipe(
      tap(friends => {
        this._friends.set(friends);
        this.cacheProfilePictures(friends);
      })
    );
  }

  loadFollowed(search?: string) {
    let params = new HttpParams();
    if (search) {
      params = params.set('q', search);
    }
    return this.http.get<Followed[]>(`${this.baseUri}/followed`, { params }).pipe(
      tap(followed => {
        this._followed.set(followed);
        this.cacheProfilePictures(followed);
      })
    );
  }

  unfriend(username: string) {
    return this.http.delete<void>(`${this.baseUri}/friends/${username}`);
  }

  follow(username: string) {
    return this.http.post<void>(`${this.baseUri}/followed/${username}`, {});
  }

  unfollow(username: string) {
    return this.http.delete<void>(`${this.baseUri}/followed/${username}`);
  }

  loadSentFriendRequests() {
    return this.http.get<FriendRequest[]>(`${this.baseUri}/friend-requests/sent`, {
      context: SILENT_ERROR_CONTEXT,
    }).pipe(
      tap(requests => {
        this._sentRequests.set(requests);
        this.cacheProfilePictures(requests);
        this.eventBus.updateState({ sent: requests });
      })
    );
  }

  loadIncomingFriendRequests() {
    return this.http.get<FriendRequest[]>(`${this.baseUri}/friend-requests/incoming`, {
      context: SILENT_ERROR_CONTEXT,
    }).pipe(
      tap(requests => {
        this._incomingRequests.set(requests);
        this.cacheProfilePictures(requests);
        this.eventBus.updateState({ incoming: requests });
      })
    );
  }

  loadPendingRequestCount() {
    return this.http.get<number>(`${this.baseUri}/friend-requests/count`, {
      context: SILENT_ERROR_CONTEXT,
    }).pipe(
      tap(count => {
        this._pendingRequestCount.set(count);
        this.eventBus.updateState({ pendingCount: count });
      }),
      catchError(() => of(this._pendingRequestCount()))
    );
  }

  sendFriendRequest(toUsername: string) {
    const request: SendFriendRequest = { toUsername };
    return this.http.post<void>(`${this.baseUri}/friend-requests`, request).pipe(
      tap(() => {
        this.eventBus.requestSent(toUsername);
      })
    );
  }

  acceptFriendRequest(username: string, request?: FriendRequest) {
    return this.http.patch<void>(`${this.baseUri}/friend-requests/${username}`, { action: 'ACCEPT' }).pipe(
      tap(() => {
        if (request) {
          this.eventBus.acceptRequest(request);
        } else {
          this.eventBus.emitAction({
            type: 'accepted',
            username,
            timestamp: new Date(),
          });
        }
      })
    );
  }

  declineFriendRequest(username: string, request?: FriendRequest) {
    return this.http.patch<void>(`${this.baseUri}/friend-requests/${username}`, { action: 'DECLINE' }).pipe(
      tap(() => {
        if (request) {
          this.eventBus.declineRequest(request);
        } else {
          this.eventBus.emitAction({
            type: 'declined',
            username,
            timestamp: new Date(),
          });
        }
      })
    );
  }

  cancelFriendRequest(username: string, request?: FriendRequest) {
    return this.http.delete<void>(`${this.baseUri}/friend-requests/${username}`).pipe(
      tap(() => {
        if (request) {
          this.eventBus.cancelRequest(request);
        } else {
          this.eventBus.emitAction({
            type: 'cancelled',
            username,
            timestamp: new Date(),
          });
          this.eventBus.requestRefresh();
        }
      })
    );
  }

  /**
   * @deprecated Use SearchService.searchUsers() instead. This method is kept for backward compatibility.
   * Searches for users using the new POST endpoint.
   */
  searchUsers(
    query: string,
    excludeFriends = false,
    excludeFollowed = false,
    verifiedOnly = false
  ) {
    const request = {
      query: query,
      excludeFriends: excludeFriends,
      excludeFollowed: excludeFollowed,
      verifiedOnly: verifiedOnly,
    };

    const searchUri = environment.apiUrl + '/search';
    return this.http.post<UserSearchResult[]>(`${searchUri}/users`, request).pipe(
      tap(results => this._searchResults.set(results))
    );
  }

  clearSearch() {
    this._searchResults.set([]);
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
  }

  private cacheProfilePictures(users: { profilePicture: string | null }[]): void {
    users.forEach(user => this.mediaCacheService.cacheUrl(user.profilePicture, 'image', 'high'));
  }

  private safeRefresh<T>(refresh$: Observable<T>) {
    return refresh$.pipe(catchError(() => of(null as T | null)));
  }
}

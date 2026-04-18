import { Injectable, inject, signal, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { tap, switchMap, takeUntil, Subject, merge } from 'rxjs';
import { environment } from '../../environments/environment';
import { Friend, FollowedUser, FriendRequest, SendFriendRequest, UserSearchResult } from './dto/Relationship';
import { FriendRequestEventBusService, FriendRequestActionEvent } from './services/friend-request-event-bus.service';

@Injectable({
  providedIn: 'root',
})
export class RelationshipService implements OnDestroy {
  private readonly baseUri: string = environment.apiUrl + '/users/me';
  private readonly searchUri: string = environment.apiUrl + '/users';
  private http = inject(HttpClient);
  private eventBus = inject(FriendRequestEventBusService);

  private _friends = signal<Friend[]>([]);
  private _following = signal<FollowedUser[]>([]);
  private _incomingRequests = signal<FriendRequest[]>([]);
  private _sentRequests = signal<FriendRequest[]>([]);
  private _pendingRequestCount = signal<number>(0);
  private _searchResults = signal<UserSearchResult[]>([]);

  public friends = this._friends.asReadonly();
  public following = this._following.asReadonly();
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

  /**
   * Handle action events and trigger appropriate follow-up refreshes
   */
  private handleActionEvent(event: FriendRequestActionEvent): void {
    switch (event.type) {
      case 'accepted':
        this.loadFriends().subscribe();
        break;
      case 'declined':
      case 'cancelled':
        break;
      case 'sent':
        this._consolidatedRefresh$.next();
        break;
    }
  }

  /**
   * Perform a consolidated refresh of all friend request data in parallel
   * Updates the event bus state with new data
   */
  private performConsolidatedRefresh() {
    this.eventBus.setLoading(true);

    return merge(
      this.loadIncomingFriendRequests(),
      this.loadSentFriendRequests(),
      this.loadPendingRequestCount()
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

  loadFriends(search?: string) {
    let params = new HttpParams();
    if (search) {
      params = params.set('q', search);
    }
    return this.http.get<Friend[]>(`${this.baseUri}/friends`, { params }).pipe(
      tap(friends => this._friends.set(friends))
    );
  }

  loadFollowing(search?: string) {
    let params = new HttpParams();
    if (search) {
      params = params.set('q', search);
    }
    return this.http.get<FollowedUser[]>(`${this.baseUri}/following`, { params }).pipe(
      tap(following => this._following.set(following))
    );
  }

  follow(username: string) {
    return this.http.post<void>(`${this.baseUri}/following/${username}`, {});
  }

  unfollow(username: string) {
    return this.http.delete<void>(`${this.baseUri}/following/${username}`);
  }

  loadSentFriendRequests() {
    return this.http.get<FriendRequest[]>(`${this.baseUri}/friend-requests/sent`).pipe(
      tap(requests => {
        this._sentRequests.set(requests);
        this.eventBus.updateState({ sent: requests });
      })
    );
  }

  loadIncomingFriendRequests() {
    return this.http.get<FriendRequest[]>(`${this.baseUri}/friend-requests/incoming`).pipe(
      tap(requests => {
        this._incomingRequests.set(requests);
        this.eventBus.updateState({ incoming: requests });
      })
    );
  }

  loadPendingRequestCount() {
    return this.http.get<number>(`${this.baseUri}/friend-requests/count`).pipe(
      tap(count => {
        this._pendingRequestCount.set(count);
        this.eventBus.updateState({ pendingCount: count });
      })
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

  acceptFriendRequest(requestId: string, request?: FriendRequest) {
    return this.http.post<void>(`${this.baseUri}/friend-requests/${requestId}/accept`, {}).pipe(
      tap(() => {
        if (request) {
          this.eventBus.acceptRequest(request);
        } else {
          this.eventBus.emitAction({
            type: 'accepted',
            requestId,
            timestamp: new Date(),
          });
          this.eventBus.requestRefresh();
        }
      })
    );
  }

  declineFriendRequest(requestId: string, request?: FriendRequest) {
    return this.http.delete<void>(`${this.baseUri}/friend-requests/${requestId}`).pipe(
      tap(() => {
        if (request) {
          this.eventBus.declineRequest(request);
        } else {
          this.eventBus.emitAction({
            type: 'declined',
            requestId,
            timestamp: new Date(),
          });
          this.eventBus.requestRefresh();
        }
      })
    );
  }

  cancelFriendRequest(request: FriendRequest) {
    return this.http.delete<void>(`${this.baseUri}/friend-requests/${request.id}`).pipe(
      tap(() => {
        this.eventBus.cancelRequest(request);
      })
    );
  }

  // potientially replaceable/reusable with a global search
  searchUsers(
    query: string,
    excludeFriends: boolean = false,
    excludeFollowing: boolean = false,
    verifiedOnly: boolean = false
  ) {
    let params = new HttpParams()
      .set('q', query)
      .set('excludeFriends', excludeFriends.toString())
      .set('excludeFollowing', excludeFollowing.toString())
      .set('verifiedOnly', verifiedOnly.toString());

    return this.http.get<UserSearchResult[]>(`${this.searchUri}/search`, { params }).pipe(
      tap(results => this._searchResults.set(results))
    );
  }

  clearSearch() {
    this._searchResults.set([]);
  }

  /**
   * Cleanup subscriptions when service is destroyed
   * Prevents memory leaks
   */
  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
  }
}

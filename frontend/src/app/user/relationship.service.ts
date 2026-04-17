import { Injectable, inject, signal, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { tap, switchMap, takeUntil, Subject, merge, forkJoin } from 'rxjs';
import { environment } from '../../environments/environment';
import { Friend, Followed, FriendRequest, SendFriendRequest, UserSearchResult } from './dto/Relationship';
import { FriendRequestEventBusService, FriendRequestActionEvent } from './friend-request-event-bus.service';

@Injectable({
  providedIn: 'root',
})
export class RelationshipService implements OnDestroy {
  private readonly baseUri: string = environment.apiUrl + '/users/me';
  private readonly searchUri: string = environment.apiUrl + '/users';
  private http = inject(HttpClient);
  private eventBus = inject(FriendRequestEventBusService);

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
        forkJoin([
          this.loadIncomingFriendRequests(),
          this.loadPendingRequestCount(),
          this.loadFriends()
        ]).pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'declined':
        forkJoin([
          this.loadIncomingFriendRequests(),
          this.loadPendingRequestCount()
        ]).pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'cancelled':
        this.loadSentFriendRequests().pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'sent':
        this.loadSentFriendRequests().pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'followed':
        this.loadFollowed().pipe(takeUntil(this._destroy$)).subscribe();
        break;
      case 'unfollowed':
        forkJoin([
          this.loadFollowed(),
          this.loadFriends()
        ]).pipe(takeUntil(this._destroy$)).subscribe();
        break;
    }
  }

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

  loadFollowed(search?: string) {
    let params = new HttpParams();
    if (search) {
      params = params.set('q', search);
    }
    return this.http.get<Followed[]>(`${this.baseUri}/followed`, { params }).pipe(
      tap(followed => this._followed.set(followed))
    );
  }

  follow(username: string) {
    return this.http.post<void>(`${this.baseUri}/followed/${username}`, {});
  }

  unfollow(username: string) {
    return this.http.delete<void>(`${this.baseUri}/followed/${username}`);
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

  /**
   * Accept an incoming friend request using the new PATCH endpoint
   * @param username - The username of the sender (from the incoming request)
   */
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
          this.eventBus.requestRefresh();
        }
      })
    );
  }

  /**
   * Decline an incoming friend request using the new PATCH endpoint
   * @param username - The username of the sender (from the incoming request)
   */
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
          this.eventBus.requestRefresh();
        }
      })
    );
  }

  /**
   * Cancel a sent friend request using the new DELETE endpoint
   * @param username - The username of the receiver (to whom the request was sent)
   */
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

  // potientially replaceable/reusable with a global search
  searchUsers(
    query: string,
    excludeFriends: boolean = false,
    excludeFollowed: boolean = false,
    verifiedOnly: boolean = false
  ) {
    let params = new HttpParams()
      .set('q', query)
      .set('excludeFriends', excludeFriends.toString())
      .set('excludeFollowed', excludeFollowed.toString())
      .set('verifiedOnly', verifiedOnly.toString());

    return this.http.get<UserSearchResult[]>(`${this.searchUri}/search`, { params }).pipe(
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
}

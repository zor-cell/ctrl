import { Injectable, OnDestroy, signal } from '@angular/core';
import { Subject, BehaviorSubject, ReplaySubject, Observable, takeUntil, shareReplay } from 'rxjs';
import { FriendRequest } from './dto/Relationship';

export interface FriendRequestActionEvent {
  type: 'accepted' | 'declined' | 'cancelled' | 'sent' | 'followed' | 'unfollowed';
  username?: string;
  timestamp: Date;
}

export interface FriendRequestState {
  incoming: FriendRequest[];
  sent: FriendRequest[];
  pendingCount: number;
  lastUpdated: Date | null;
  isLoading: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class FriendRequestEventBusService implements OnDestroy {
  private _state = new BehaviorSubject<FriendRequestState>({
    incoming: [],
    sent: [],
    pendingCount: 0,
    lastUpdated: null,
    isLoading: false,
  });

  private _actionEvents = new ReplaySubject<FriendRequestActionEvent>(1);
  private _refreshRequests = new Subject<void>();
  private _destroy$ = new Subject<void>();

  public state$: Observable<FriendRequestState> = this._state.pipe(
    takeUntil(this._destroy$),
    shareReplay(1)
  );

  public actionEvents$: Observable<FriendRequestActionEvent> = this._actionEvents.pipe(
    takeUntil(this._destroy$),
    shareReplay(1)
  );

  public refreshRequests$: Observable<void> = this._refreshRequests.pipe(
    takeUntil(this._destroy$),
    shareReplay(1)
  );

  public readonly state = signal<FriendRequestState>(this._state.getValue());

  constructor() {
    // Sync BehaviorSubject to signal for components that prefer signals
    this._state.pipe(takeUntil(this._destroy$)).subscribe((newState) => {
      this.state.set(newState);
    });
  }

  updateState(partialState: Partial<FriendRequestState>): void {
    const current = this._state.getValue();
    this._state.next({
      ...current,
      ...partialState,
      lastUpdated: new Date(),
    });
  }

  emitAction(event: FriendRequestActionEvent): void {
    this._actionEvents.next(event);
  }

  requestRefresh(): void {
    this._refreshRequests.next();
  }

  setLoading(isLoading: boolean): void {
    const current = this._state.getValue();
    this._state.next({ ...current, isLoading });
  }

  acceptRequest(request: FriendRequest): void {
    // For incoming requests, username is the sender's username
    this.emitAction({
      type: 'accepted',
      username: request.username,
      timestamp: new Date(),
    });
  }

  declineRequest(request: FriendRequest): void {
    // For incoming requests, username is the sender's username
    this.emitAction({
      type: 'declined',
      username: request.username,
      timestamp: new Date(),
    });
  }

  cancelRequest(request: FriendRequest): void {
    // For sent requests, username is the receiver's username
    this.emitAction({
      type: 'cancelled',
      username: request.username,
      timestamp: new Date(),
    });
  }

  requestSent(username: string): void {
    this.emitAction({
      type: 'sent',
      username,
      timestamp: new Date(),
    });
  }

  followed(username: string): void {
    this.emitAction({
      type: 'followed',
      username,
      timestamp: new Date(),
    });
  }

  unfollowed(): void {
    this.emitAction({
      type: 'unfollowed',
      timestamp: new Date(),
    });
  }

  getCurrentState(): FriendRequestState {
    return this._state.getValue();
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
    this._state.complete();
    this._actionEvents.complete();
    this._refreshRequests.complete();
  }
}

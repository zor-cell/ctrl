import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Subject, BehaviorSubject, ReplaySubject, Observable, takeUntil, shareReplay } from 'rxjs';
import { FriendRequest } from '../dto/Relationship';

export interface FriendRequestActionEvent {
  type: 'accepted' | 'declined' | 'cancelled' | 'sent' | 'followed' | 'unfollowed';
  requestId: string;
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

/**
 * EventBus service for managing friend request state and communication
 * between components. Provides:
 * - Centralized state management via RxJS subjects
 * - Action event streams for cross-component communication
 * - Auto-cleanup on service destruction
 * - ShareReplay for efficient multicasting to multiple subscribers
 */
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
    this.emitAction({
      type: 'accepted',
      requestId: request.id,
      username: request.username,
      timestamp: new Date(),
    });
    this.requestRefresh();
  }

  declineRequest(request: FriendRequest): void {
    this.emitAction({
      type: 'declined',
      requestId: request.id,
      username: request.username,
      timestamp: new Date(),
    });
    this.requestRefresh();
  }

  cancelRequest(request: FriendRequest): void {
    this.emitAction({
      type: 'cancelled',
      requestId: request.id,
      username: request.username,
      timestamp: new Date(),
    });
    this.requestRefresh();
  }

  requestSent(username: string): void {
    this.emitAction({
      type: 'sent',
      requestId: '',
      username,
      timestamp: new Date(),
    });
    this.requestRefresh();
  }

  followed(): void {
    this.emitAction({
      type: 'followed',
      requestId: '',
      timestamp: new Date(),
    });
  }

  unfollowed(): void {
    this.emitAction({
      type: 'unfollowed',
      requestId: '',
      timestamp: new Date(),
    });
  }

  getCurrentState(): FriendRequestState {
    return this._state.getValue();
  }

  /**
   * Cleanup all subscriptions when service is destroyed
   * Prevents memory leaks by completing all subjects
   */
  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
    this._state.complete();
    this._actionEvents.complete();
    this._refreshRequests.complete();
  }
}

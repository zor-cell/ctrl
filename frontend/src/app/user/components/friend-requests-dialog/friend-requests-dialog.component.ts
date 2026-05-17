import { Component, inject, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Button } from 'primeng/button';
import { Divider } from 'primeng/divider';
import { Subject, takeUntil } from 'rxjs';

import { RelationshipService } from '../../relationship.service';
import { NotificationService } from '../../../core/services/notification.service';
import { FriendRequestEventBusService } from '../../friend-request-event-bus.service';
import { FriendRequest } from '../../dto/Relationship';

/**
 * Friend Requests Dialog Component
 *
 * Uses the FriendRequestEventBus for:
 * - State management (shared with other components)
 * - Auto-refresh on actions (no manual reload calls needed)
 * - Memory-leak protected subscriptions
 */
@Component({
  selector: 'app-friend-requests-dialog',
  standalone: true,
  imports: [CommonModule, Button, Divider],
  templateUrl: './friend-requests-dialog.component.html',
  styleUrl: './friend-requests-dialog.component.scss',
})
export class FriendRequestsDialogComponent implements OnInit, OnDestroy {
  private relationshipService = inject(RelationshipService);
  private notificationService = inject(NotificationService);
  private eventBus = inject(FriendRequestEventBusService);

  @Output() closeEvent = new EventEmitter<void>();

  incomingRequests = this.relationshipService.incomingRequests;
  sentRequests = this.relationshipService.sentRequests;

  private _destroy$ = new Subject<void>();

  isLoading = this.eventBus.state().isLoading;

  ngOnInit(): void {
    this.eventBus.state$
      .pipe(takeUntil(this._destroy$))
      .subscribe((state) => {
        this.isLoading = state.isLoading;
      });

    this.eventBus.requestRefresh();
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
  }

  acceptRequest(request: FriendRequest) {
    // For incoming requests, username is the sender's username
    this.relationshipService.acceptFriendRequest(request.username, request).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Accepted friend request from ${request.username}`);
      },
      error: () => {
        this.notificationService.showError('Failed to accept friend request');
      },
    });
  }

  declineRequest(request: FriendRequest) {
    // For incoming requests, username is the sender's username
    this.relationshipService.declineFriendRequest(request.username, request).subscribe({
      next: () => {
        this.notificationService.showSuccess('Friend request declined');
      },
      error: () => {
        this.notificationService.showError('Failed to decline friend request');
      },
    });
  }

  cancelRequest(request: FriendRequest) {
    // For sent requests, username is the receiver's username
    this.relationshipService.cancelFriendRequest(request.username, request).subscribe({
      next: () => {
        this.notificationService.showSuccess('Friend request cancelled');
      },
      error: () => {
        this.notificationService.showError('Failed to cancel friend request');
      },
    });
  }

  close() {
    this.closeEvent.emit();
  }
}

import { Component, effect, EventEmitter, inject, OnDestroy, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Button } from 'primeng/button';
import { Divider } from 'primeng/divider';
import { Subject, takeUntil } from 'rxjs';

import { RelationshipService } from '../../relationship.service';
import { NotificationService } from '../../../core/services/notification.service';
import { FriendRequestEventBusService } from '../../friend-request-event-bus.service';
import { FriendRequest } from '../../dto/Relationship';
import { MediaCacheService } from '../../../core/services/media-cache.service';
import { usernameInitial } from '../../../core/utils/username-initial.utils';

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
  private mediaCacheService = inject(MediaCacheService);

  @Output() closeEvent = new EventEmitter<void>();

  incomingRequests = this.relationshipService.incomingRequests;
  sentRequests = this.relationshipService.sentRequests;
  profilePictureUrls = signal<Record<string, string>>({});
  failedProfilePictures = signal<Set<string>>(new Set());

  private _destroy$ = new Subject<void>();

  isLoading = this.eventBus.state().isLoading;

  constructor() {
    effect(() => {
      this.loadProfilePictures([
        ...this.incomingRequests(),
        ...this.sentRequests(),
      ]);
    });
  }

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

  hasProfilePicture(request: FriendRequest): boolean {
    return Boolean(this.profilePictureUrl(request))
      && !this.failedProfilePictures().has(request.username);
  }

  profilePictureUrl(request: FriendRequest): string | null {
    return this.profilePictureUrls()[request.username]
      ?? this.directProfilePictureUrl(request.profilePicture);
  }

  markProfilePictureFailed(request: FriendRequest): void {
    this.failedProfilePictures.update(failedUsernames => {
      const nextFailedUsernames = new Set(failedUsernames);
      nextFailedUsernames.add(request.username);
      return nextFailedUsernames;
    });
  }

  getInitial(request: FriendRequest): string {
    return usernameInitial(request.username);
  }

  private loadProfilePictures(requests: FriendRequest[]): void {
    const activeUsernames = new Set(requests.map(request => request.username));
    this.profilePictureUrls.update(urls => {
      const nextUrls: Record<string, string> = {};
      Object.entries(urls).forEach(([username, url]) => {
        if (activeUsernames.has(username)) {
          nextUrls[username] = url;
        }
      });
      return nextUrls;
    });

    requests.forEach(request => {
      if (!request.profilePicture) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(request.profilePicture, 'image', 'high').subscribe({
        next: url => {
          if (url) {
            this.profilePictureUrls.update(urls => ({
              ...urls,
              [request.username]: url,
            }));
          }
        },
        error: () => this.markProfilePictureFailed(request),
      });
    });
  }

  private directProfilePictureUrl(url: string | null): string | null {
    if (!url || this.mediaCacheService.isBackendManagedMediaUrl(url)) {
      return null;
    }

    return url;
  }
}

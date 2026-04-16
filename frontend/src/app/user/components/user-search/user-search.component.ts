import { Component, inject, input, signal, effect, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil, merge, filter } from 'rxjs';

import { InputText } from 'primeng/inputtext';
import { Button } from 'primeng/button';

import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Avatar } from 'primeng/avatar';

import { RelationshipService } from '../../relationship.service';
import { FriendRequestEventBusService } from '../../services/friend-request-event-bus.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserSearchResult } from '../../dto/Relationship';

/**
 * User Search Component
 *
 * Features:
 * - Debounced search (300ms after typing stops)
 * - Filter toggles for friends/following/verified
 * - Context-aware action buttons based on relationship status
 * - Accept incoming friend requests directly from search
 * - Auto-refresh on relationship changes via event bus
 */
@Component({
  selector: 'app-user-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    InputText,
    Button,
    IconField,
    InputIcon,
    Avatar,
  ],
  templateUrl: './user-search.component.html',
  styleUrl: './user-search.component.scss',
})
export class UserSearchComponent implements OnInit, OnDestroy {
  private socialService = inject(RelationshipService);
  private notificationService = inject(NotificationService);
  private eventBus = inject(FriendRequestEventBusService);

  source = input.required<'friends' | 'following'>();

  searchQuery = signal<string>('');
  excludeFriends = signal<boolean>(true);
  excludeFollowing = signal<boolean>(false);
  verifiedOnly = signal<boolean>(false);

  searchResults = this.socialService.searchResults;

  private searchInput$ = new Subject<string>();
  private refreshTrigger$ = new Subject<void>();
  private _destroy$ = new Subject<void>();

  constructor() {
    effect(() => {
      const src = this.source();
      if (src === 'friends') {
        this.excludeFriends.set(true);
        this.excludeFollowing.set(false);
        this.verifiedOnly.set(false);
      } else {
        this.excludeFriends.set(false);
        this.excludeFollowing.set(true);
        this.verifiedOnly.set(true);
      }
    });
  }

  ngOnInit(): void {
    this.searchInput$
      .pipe(
        takeUntil(this._destroy$),
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe((query) => {
        this.performSearch(query);
      });

    this.refreshTrigger$
      .pipe(
        takeUntil(this._destroy$),
        debounceTime(100)
      )
      .subscribe(() => {
        if (this.searchQuery().trim().length >= 2) {
          this.performSearch(this.searchQuery().trim());
        }
      });

    this.eventBus.actionEvents$
      .pipe(
        takeUntil(this._destroy$),
        filter((event) =>
          ['accepted', 'declined', 'cancelled', 'sent', 'followed', 'unfollowed'].includes(event.type)
        )
      )
      .subscribe(() => {
        this.refreshTrigger$.next();
      });
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
    this.searchInput$.complete();
    this.refreshTrigger$.complete();
  }

  onSearchInputChange(query: string): void {
    this.searchQuery.set(query);
    if (query.trim().length >= 2) {
      this.searchInput$.next(query.trim());
    } else {
      this.socialService.clearSearch();
    }
  }

  onSearch(): void {
    const query = this.searchQuery().trim();
    this.searchInput$.next(query);
  }

  private performSearch(query: string): void {
    if (query.length < 2) {
      this.socialService.clearSearch();
      return;
    }

    this.socialService
      .searchUsers(
        query,
        this.excludeFriends(),
        this.excludeFollowing(),
        this.verifiedOnly()
      )
      .pipe(takeUntil(this._destroy$))
      .subscribe({
        error: () => {
          this.notificationService.showError('Search failed');
        },
      });
  }

  toggleFilter(filter: 'friends' | 'following' | 'verified') {
    switch (filter) {
      case 'friends':
        this.excludeFriends.update(v => !v);
        break;
      case 'following':
        this.excludeFollowing.update(v => !v);
        break;
      case 'verified':
        this.verifiedOnly.update(v => !v);
        break;
    }
    const query = this.searchQuery().trim();
    if (query.length >= 2) {
      this.performSearch(query);
    }
  }

  followUser(user: UserSearchResult) {
    this.socialService.follow(user.username).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Now following ${user.username}`);
        this.eventBus.followed();
      },
      error: () => {
        this.notificationService.showError('Failed to follow user');
      },
    });
  }

  sendFriendRequest(user: UserSearchResult) {
    this.socialService.sendFriendRequest(user.username).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Friend request sent to ${user.username}`);
      },
      error: () => {
        this.notificationService.showError('Failed to send friend request');
      },
    });
  }

  acceptIncomingRequest(user: UserSearchResult) {
    // Find the request ID from the pending incoming requests
    // Since we don't have the request ID in the search result,
    // we need to use a different approach - find it via the event bus or service
    this.socialService.loadIncomingFriendRequests()
      .pipe(takeUntil(this._destroy$))
      .subscribe({
        next: (requests) => {
          const matchingRequest = requests.find(r => r.username === user.username);
          if (matchingRequest) {
            this.socialService.acceptFriendRequest(matchingRequest.id, matchingRequest).subscribe({
              next: () => {
                this.notificationService.showSuccess(`Accepted friend request from ${user.username}`);
              },
              error: () => {
                this.notificationService.showError('Failed to accept friend request');
              },
            });
          } else {
            this.notificationService.showError('Friend request not found');
            this.onSearch();
          }
        },
        error: () => {
          this.notificationService.showError('Failed to find friend request');
        },
      });
  }

  canFollow(user: UserSearchResult): boolean {
    return user.relationshipStatus === 'NONE' && user.isVerified;
  }

  canAddFriend(user: UserSearchResult): boolean {
    return user.relationshipStatus !== 'FRIEND' &&
           user.relationshipStatus !== 'PENDING_REQUEST_SENT' &&
           user.relationshipStatus !== 'PENDING_REQUEST_INCOMING';
  }

  hasIncomingRequest(user: UserSearchResult): boolean {
    return user.relationshipStatus === 'PENDING_REQUEST_INCOMING';
  }

  hasPendingSentRequest(user: UserSearchResult): boolean {
    return user.relationshipStatus === 'PENDING_REQUEST_SENT';
  }
}

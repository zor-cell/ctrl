import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { firstValueFrom } from 'rxjs';

import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { AvatarModule } from 'primeng/avatar';

import { SearchService } from '../../services/search.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserSearchResult } from '../../../user/dto/Relationship';
import { FriendRequestEventBusService } from '../../../user/friend-request-event-bus.service';
import { RelationshipService } from '../../../user/relationship.service';
import { MediaCacheService } from '../../../core/services/media-cache.service';

/**
 * Users Search Component
 *
 * Features:
 * - Search by username (triggered by button click or Enter key)
 * - Filter toggles for friends/following/verified
 * - Context-aware action buttons based on relationship status
 * - Accept incoming friend requests directly from search
 * - Auto-refresh on relationship changes via event bus
 *
 * Default filters: excludeFriends=true, excludeFollowed=true
 */
@Component({
  selector: 'app-users-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    InputTextModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    AvatarModule,
  ],
  templateUrl: './users-search.component.html',
  styleUrl: './users-search.component.scss',
})
export class UsersSearchComponent implements OnInit, OnDestroy {
  private searchService = inject(SearchService);
  private notificationService = inject(NotificationService);
  private eventBus = inject(FriendRequestEventBusService);
  private relationshipService = inject(RelationshipService);
  private mediaCache = inject(MediaCacheService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  searchQuery = signal<string>('');
  excludeFriends = signal<boolean>(false);
  excludeFollowed = signal<boolean>(false);
  verifiedOnly = signal<boolean>(false);

  searchResults = signal<UserSearchResult[]>([]);
  isSearching = signal<boolean>(false);
  resolvedAvatars = signal<Record<string, string | null>>({});

  private _destroy$ = new Subject<void>();
  private refreshTrigger$ = new Subject<void>();
  private searchInput$ = new Subject<string>();

  ngOnInit(): void {
    // Restore state from URL first
    this.restoreStateFromUrl();

    // Debounced search input (300ms)
    this.searchInput$
      .pipe(
        takeUntil(this._destroy$),
        debounceTime(300)
      )
      .subscribe((query) => {
        this.updateUrlState();
        if (query.length >= 2) {
          this.performSearch(query);
        }
      });

    // Auto-refresh when relationship changes
    this.refreshTrigger$
      .pipe(takeUntil(this._destroy$))
      .subscribe(() => {
        const query = this.searchQuery().trim();
        if (query.length >= 2) {
          this.performSearch(query);
        }
      });

    // Listen for relationship changes
    this.eventBus.actionEvents$
      .pipe(takeUntil(this._destroy$))
      .subscribe(() => {
        this.refreshTrigger$.next();
      });
  }

  /**
   * Restore search state from URL query parameters
   */
  private restoreStateFromUrl(): void {
    const queryParams = this.route.snapshot.queryParams;

    // Restore search query
    if (queryParams['q']) {
      this.searchQuery.set(queryParams['q']);
    }

    // Restore filter states
    if (queryParams['friends'] !== undefined) {
      this.excludeFriends.set(queryParams['friends'] !== 'false');
    }
    if (queryParams['followed'] !== undefined) {
      this.excludeFollowed.set(queryParams['followed'] !== 'false');
    }
    if (queryParams['verified'] !== undefined) {
      this.verifiedOnly.set(queryParams['verified'] === 'true');
    }

    // If we have a query, perform the search
    if (this.searchQuery().trim().length >= 2) {
      this.performSearch(this.searchQuery().trim());
    }
  }

  /**
   * Update URL with current search state
   */
  private updateUrlState(): void {
    const queryParams: Record<string, string> = {};

    if (this.searchQuery()) {
      queryParams['q'] = this.searchQuery();
    }

    // Always include filter states if they differ from defaults
    if (!this.excludeFriends()) {
      queryParams['friends'] = 'false';
    }
    if (!this.excludeFollowed()) {
      queryParams['followed'] = 'false';
    }
    if (this.verifiedOnly()) {
      queryParams['verified'] = 'true';
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: Object.keys(queryParams).length > 0 ? queryParams : undefined,
      replaceUrl: true
    });
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
    this.refreshTrigger$.complete();
    this.searchInput$.complete();
  }

  onSearchInputChange(query: string): void {
    this.searchQuery.set(query);
    this.searchInput$.next(query.trim());
  }

  canSearch(): boolean {
    return this.searchQuery().trim().length >= 2;
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.searchResults.set([]);

    // Clear URL parameters
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: undefined,
      replaceUrl: true
    });
  }

  onSearch(): void {
    const query = this.searchQuery().trim();
    if (query.length < 2) {
      this.notificationService.showError('Please enter at least 2 characters');
      return;
    }
    this.updateUrlState();
    this.performSearch(query);
  }

  private performSearch(query: string): void {
    this.isSearching.set(true);

    const request = {
      query: query,
      excludeFriends: this.excludeFriends(),
      excludeFollowed: this.excludeFollowed(),
      verifiedOnly: this.verifiedOnly(),
    };

    this.searchService
      .searchUsers(request)
      .pipe(takeUntil(this._destroy$))
      .subscribe({
        next: (results) => {
          this.searchResults.set(results);
          this.isSearching.set(false);
          // Update URL with current scroll position after search
          this.updateUrlState();
          // Resolve profile picture URLs via media cache
          this.resolveAvatars(results);
        },
        error: () => {
          this.notificationService.showError('Search failed');
          this.isSearching.set(false);
        },
      });
  }

  toggleFilter(filter: 'friends' | 'followed' | 'verified') {
    switch (filter) {
      case 'friends':
        this.excludeFriends.update(v => !v);
        break;
      case 'followed':
        this.excludeFollowed.update(v => !v);
        break;
      case 'verified':
        this.verifiedOnly.update(v => !v);
        break;
    }

    // Update URL with new filter state
    this.updateUrlState();

    // Re-search if there's a query
    const query = this.searchQuery().trim();
    if (query.length >= 2) {
      this.performSearch(query);
    }
  }

  followUser(user: UserSearchResult) {
    this.relationshipService.follow(user.username).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Now following ${user.username}`);
        this.eventBus.followed(user.username);
      },
      error: () => {
        this.notificationService.showError('Failed to follow user');
      },
    });
  }

  sendFriendRequest(user: UserSearchResult) {
    this.relationshipService.sendFriendRequest(user.username).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Friend request sent to ${user.username}`);
        // Refresh the search results to show the updated status
        const query = this.searchQuery().trim();
        if (query.length >= 2) {
          this.performSearch(query);
        }
      },
      error: () => {
        this.notificationService.showError('Failed to send friend request');
      },
    });
  }

  acceptIncomingRequest(user: UserSearchResult) {
    this.relationshipService.acceptFriendRequest(user.username).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Accepted friend request from ${user.username}`);
        // Refresh the search results
        const query = this.searchQuery().trim();
        if (query.length >= 2) {
          this.performSearch(query);
        }
      },
      error: () => {
        this.notificationService.showError('Failed to accept friend request');
      },
    });
  }

  private hasStatus(user: UserSearchResult, status: string): boolean {
    return user.relationshipStatuses.some(r => r.status === status);
  }

  canFollow(user: UserSearchResult): boolean {
    return !this.hasStatus(user, 'FRIEND') &&
      !this.hasStatus(user, 'FOLLOWED') &&
      user.isVerified;
  }

  canAddFriend(user: UserSearchResult): boolean {
    return !this.hasStatus(user, 'FRIEND') &&
      !this.hasStatus(user, 'PENDING_REQUEST_SENT') &&
      !this.hasStatus(user, 'PENDING_REQUEST_INCOMING');
  }

  hasIncomingRequest(user: UserSearchResult): boolean {
    return this.hasStatus(user, 'PENDING_REQUEST_INCOMING');
  }

  hasPendingSentRequest(user: UserSearchResult): boolean {
    return this.hasStatus(user, 'PENDING_REQUEST_SENT');
  }

  navigateToProfile(username: string): void {
    this.router.navigate(['/profile', username]);
  }

  private async resolveAvatars(users: UserSearchResult[]): Promise<void> {
    const usersWithPicture = users.filter(u => u.profilePicture);
    const entries = await Promise.all(
      usersWithPicture.map(async (u) => {
        const resolved = await this.resolveMedia(u.profilePicture);
        return [u.id, resolved] as [string, string | null];
      })
    );
    this.resolvedAvatars.set(Object.fromEntries(entries));
  }

  private resolveMedia(url: string | null | undefined): Promise<string | null> {
    if (!url) return Promise.resolve(null);
    const kind = /\.(mp4|webm|mov)(\?|#|$)/i.test(url) ? 'video' : 'image';
    return firstValueFrom(this.mediaCache.resolveMediaUrl(url, kind, 'high')).catch(() => null);
  }

  getAvatar(user: UserSearchResult): string | undefined {
    return this.resolvedAvatars()[user.id] ?? undefined;
  }

  /**
   * Get the primary relationship status for display purposes.
   * Returns the most relevant status (FRIEND > PENDING_REQUEST_INCOMING > PENDING_REQUEST_SENT > FOLLOWED)
   */
  getRelationshipStatus(user: UserSearchResult): string | null {
    if (this.hasStatus(user, 'FRIEND')) return 'FRIEND';
    if (this.hasStatus(user, 'PENDING_REQUEST_INCOMING')) return 'PENDING_REQUEST_INCOMING';
    if (this.hasStatus(user, 'PENDING_REQUEST_SENT')) return 'PENDING_REQUEST_SENT';
    if (this.hasStatus(user, 'FOLLOWED')) return 'FOLLOWED';
    return null;
  }
}

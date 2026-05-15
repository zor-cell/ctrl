import { Component, inject, input, signal, effect, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, takeUntil, filter } from 'rxjs';

import { InputText } from 'primeng/inputtext';
import { Button } from 'primeng/button';

import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

import { RelationshipService } from '../../relationship.service';
import { FriendRequestEventBusService } from '../../friend-request-event-bus.service';
import { MediaCacheService } from '../../../core/services/media-cache.service';
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
  ],
  templateUrl: './user-search.component.html',
  styleUrl: './user-search.component.scss',
})
export class UserSearchComponent implements OnInit, OnDestroy {
  private relationshipService = inject(RelationshipService);
  private mediaCacheService = inject(MediaCacheService);
  private notificationService = inject(NotificationService);
  private eventBus = inject(FriendRequestEventBusService);
  private router = inject(Router);

  source = input.required<'friends' | 'followed'>();

  searchQuery = signal<string>('');
  excludeFriends = signal<boolean>(true);
  excludeFollowed = signal<boolean>(false);
  verifiedOnly = signal<boolean>(false);

  searchResults = this.relationshipService.searchResults;
  failedProfilePictures = signal<Set<string>>(new Set());
  profilePictureUrls = signal<Record<string, string>>({});
  acceptedFriendUsernames = signal<Set<string>>(new Set());

  private searchInput$ = new Subject<string>();
  private refreshTrigger$ = new Subject<void>();
  private _destroy$ = new Subject<void>();

  constructor() {
    effect(() => {
      const src = this.source();
      if (src === 'friends') {
        this.excludeFriends.set(true);
        this.excludeFollowed.set(false);
        this.verifiedOnly.set(false);
      } else {
        this.excludeFriends.set(true);
        this.excludeFollowed.set(true);
        this.verifiedOnly.set(true);
      }
    });
  }

  ngOnInit(): void {
    this.searchInput$
      .pipe(
        takeUntil(this._destroy$),
        debounceTime(300)
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
          ['declined', 'cancelled', 'sent', 'followed', 'unfollowed'].includes(event.type)
        )
      )
      .subscribe(() => {
        this.refreshTrigger$.next();
      });

    const savedQuery = this.searchQuery().trim();
    if (savedQuery.length >= 2) {
      this.performSearch(savedQuery);
    }
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
      this.relationshipService.clearSearch();
    }
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.relationshipService.clearSearch();
  }

  onSearch(): void {
    const query = this.searchQuery().trim();
    this.searchInput$.next(query);
  }

  private performSearch(query: string): void {
    if (query.length < 2) {
      this.relationshipService.clearSearch();
      return;
    }

    this.relationshipService
      .searchUsers(
        query,
        this.excludeFriends(),
        this.excludeFollowed(),
        this.verifiedOnly()
      )
      .pipe(takeUntil(this._destroy$))
      .subscribe({
        next: users => this.loadProfilePictures(users),
        error: () => {
          this.notificationService.showError('Search failed');
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
      },
      error: () => {
        this.notificationService.showError('Failed to send friend request');
      },
    });
  }

  acceptIncomingRequest(user: UserSearchResult) {
    const username = user.username;

    this.markUserAsFriend(username);

    this.relationshipService.acceptFriendRequest(username).subscribe({
      next: () => {
        this.notificationService.showSuccess(`Accepted friend request from ${username}`);
      },
      error: () => {
        this.unmarkUserAsFriend(username);
        this.notificationService.showError('Failed to accept friend request');
      },
    });
  }

  private hasStatus(user: UserSearchResult, status: string): boolean {
    return user.relationshipStatuses.some(item => item.status === status);
  }

  private markUserAsFriend(username: string): void {
    this.acceptedFriendUsernames.update(usernames => {
      const updatedUsernames = new Set(usernames);
      updatedUsernames.add(username.toLowerCase());
      return updatedUsernames;
    });
    this.relationshipService.markSearchResultAsFriend(username);
  }

  private unmarkUserAsFriend(username: string): void {
    this.acceptedFriendUsernames.update(usernames => {
      const updatedUsernames = new Set(usernames);
      updatedUsernames.delete(username.toLowerCase());
      return updatedUsernames;
    });
    this.refreshTrigger$.next();
  }

  private wasAcceptedAsFriend(user: UserSearchResult): boolean {
    return this.acceptedFriendUsernames().has(user.username.toLowerCase());
  }

  openUserProfile(user: UserSearchResult) {
    this.router.navigate(['/profile', user.username]);
  }

  hasProfilePicture(user: UserSearchResult): boolean {
    return Boolean(this.profilePictureUrl(user)) && !this.failedProfilePictures().has(user.id);
  }

  profilePictureUrl(user: UserSearchResult): string | null {
    return this.profilePictureUrls()[user.id]
      ?? this.directProfilePictureUrl(user.profilePicture)
      ?? this.defaultProfilePictureUrl(user);
  }

  markProfilePictureFailed(user: UserSearchResult): void {
    this.failedProfilePictures.update(failedIds => {
      const nextFailedIds = new Set(failedIds);
      nextFailedIds.add(user.id);
      return nextFailedIds;
    });
  }

  getInitial(user: UserSearchResult): string {
    return (user.name?.charAt(0) || user.username.charAt(0)).toUpperCase();
  }

  canFollow(user: UserSearchResult): boolean {
    // Can follow if not already following, not a friend, and not verified (verified users can be followed)
    // Actually, looking at the original logic: it was 'NONE' && isVerified
    // So we can follow if: isVerified AND not FOLLOWED AND not FRIEND AND no pending requests
    const hasExistingRelation = this.hasStatus(user, 'FOLLOWED') ||
                                  this.isFriend(user) ||
                                  this.hasStatus(user, 'PENDING_REQUEST_SENT') ||
                                  this.hasStatus(user, 'PENDING_REQUEST_INCOMING');
    return user.isVerified && !hasExistingRelation;
  }

  canAddFriend(user: UserSearchResult): boolean {
    // Can add friend if not already friends and no pending requests
    return !this.isFriend(user) &&
           !this.hasStatus(user, 'PENDING_REQUEST_SENT') &&
           !this.hasStatus(user, 'PENDING_REQUEST_INCOMING');
  }

  hasIncomingRequest(user: UserSearchResult): boolean {
    return !this.wasAcceptedAsFriend(user) && this.hasStatus(user, 'PENDING_REQUEST_INCOMING');
  }

  hasPendingSentRequest(user: UserSearchResult): boolean {
    return this.hasStatus(user, 'PENDING_REQUEST_SENT');
  }

  isFriend(user: UserSearchResult): boolean {
    return this.wasAcceptedAsFriend(user) || this.hasStatus(user, 'FRIEND');
  }

  navigateToProfile(username: string): void {
    this.router.navigate(['/profile', username]);
  }

  private loadProfilePictures(users: UserSearchResult[]): void {
    const activeIds = new Set(users.map(user => user.id));
    this.profilePictureUrls.update(urls => {
      const nextUrls: Record<string, string> = {};
      Object.entries(urls).forEach(([id, url]) => {
        if (activeIds.has(id)) {
          nextUrls[id] = url;
        }
      });
      return nextUrls;
    });

    users.forEach(user => {
      if (!user.profilePicture) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(user.profilePicture, 'image', 'high').subscribe({
        next: url => {
          if (url) {
            this.profilePictureUrls.update(urls => ({ ...urls, [user.id]: url }));
          }
        },
        error: () => this.markProfilePictureFailed(user),
      });
    });
  }

  private directProfilePictureUrl(url: string | null): string | null {
    if (!url || this.mediaCacheService.isBackendManagedMediaUrl(url)) {
      return null;
    }

    return url;
  }

  private defaultProfilePictureUrl(user: UserSearchResult): string {
    const avatarIndex = this.avatarIndex(user.id || user.username);
    return `/assets/users/user${avatarIndex}ProfilePic.jpeg`;
  }

  private avatarIndex(seed: string): number {
    const sum = Array.from(seed).reduce((total, char) => total + char.charCodeAt(0), 0);
    return (sum % 3) + 1;
  }
}

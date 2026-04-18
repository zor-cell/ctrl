import { Component, inject, signal, OnInit, OnDestroy, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { Tabs, TabList, Tab } from 'primeng/tabs';
import { Button } from 'primeng/button';
import { Badge } from 'primeng/badge';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Dialog } from 'primeng/dialog';

import { RelationshipService } from '../../relationship.service';
import { FriendRequestEventBusService } from '../../services/friend-request-event-bus.service';
import { FriendListComponent } from '../friend-list/friend-list.component';
import { FollowingListComponent } from '../following-list/following-list.component';
import { FriendRequestsDialogComponent } from '../friend-requests-dialog/friend-requests-dialog.component';
import { UserSearchComponent } from '../user-search/user-search.component';

/**
 * Following Page Component
 *
 * Uses the FriendRequestEventBus for:
 * - Centralized refresh requests when dialogs open
 * - Shared state updates across components
 * - Memory-leak protected subscriptions
 */
@Component({
  selector: 'app-following-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    Tabs,
    TabList,
    Tab,
    Button,
    Badge,
    InputText,
    IconField,
    InputIcon,
    Dialog,
    FriendListComponent,
    FollowingListComponent,
    FriendRequestsDialogComponent,
    UserSearchComponent,
  ],
  templateUrl: './following-page.component.html',
  styleUrl: './following-page.component.scss',
})
export class FollowingPageComponent implements OnInit, OnDestroy {
  protected socialService: RelationshipService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);
  private router: Router = inject(Router);

  activeTab = signal<string>('friends');
  showRequestsDialog = signal<boolean>(false);
  showSearchDialog = signal<boolean>(false);
  searchQuery = signal<string>('');
  searchSource = signal<'friends' | 'following'>('friends');

  friendSearch = signal<string>('');
  followingSearch = signal<string>('');

  pendingCount = computed(() => this.socialService.pendingRequestCount());

  private _destroy$ = new Subject<void>();

  ngOnInit() {
    this.loadInitialData();

    this.eventBus.state$
      .pipe(takeUntil(this._destroy$))
      .subscribe((state) => {
      });
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
  }

  private loadInitialData() {
    this.socialService.loadFriends().pipe(takeUntil(this._destroy$)).subscribe();
    this.socialService.loadFollowing().pipe(takeUntil(this._destroy$)).subscribe();
    this.socialService.loadPendingRequestCount().pipe(takeUntil(this._destroy$)).subscribe();
  }

  onFriendSearchChange(query: string) {
    this.friendSearch.set(query);
    this.socialService.loadFriends(query || undefined)
      .pipe(takeUntil(this._destroy$))
      .subscribe();
  }

  onFollowingSearchChange(query: string) {
    this.followingSearch.set(query);
    this.socialService.loadFollowing(query || undefined)
      .pipe(takeUntil(this._destroy$))
      .subscribe();
  }

  openRequestsDialog() {
    this.showRequestsDialog.set(true);
    this.eventBus.requestRefresh();
  }

  closeRequestsDialog() {
    this.showRequestsDialog.set(false);
  }

  openSearchDialog(fromTab: 'friends' | 'following') {
    this.searchSource.set(fromTab);
    this.showSearchDialog.set(true);
  }

  closeSearchDialog() {
    this.showSearchDialog.set(false);
    this.socialService.clearSearch();
    this.loadInitialData();
  }
}

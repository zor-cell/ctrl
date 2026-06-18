import { Component, inject, signal, OnInit, OnDestroy, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

import { Tabs, TabList, Tab } from 'primeng/tabs';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Dialog } from 'primeng/dialog';

import { RelationshipService } from '../../relationship.service';
import { FriendRequestEventBusService } from '../../friend-request-event-bus.service';
import { FriendListComponent } from '../friend-list/friend-list.component';
import { FollowedListComponent } from '../followed-list/followed-list.component';
import { FriendRequestsDialogComponent } from '../friend-requests-dialog/friend-requests-dialog.component';
import { UsersSearchComponent } from '../../../search/components/users-search/users-search.component';
import {BlockedListComponent} from '../blocked-list/blocked-list.component';

/**
 * Followed Page Component
 *
 * Uses the FriendRequestEventBus for:
 * - Centralized refresh requests when dialogs open
 * - Shared state updates across components
 * - Memory-leak protected subscriptions
 */
@Component({
  selector: 'app-followed-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    Tabs,
    TabList,
    Tab,
    Button,
    InputText,
    IconField,
    InputIcon,
    Dialog,
    FriendListComponent,
    FollowedListComponent,
    FriendRequestsDialogComponent,
    UsersSearchComponent,
    BlockedListComponent,
  ],
  templateUrl: './followed-page.component.html',
  styleUrl: './followed-page.component.scss',
})
export class FollowedPageComponent implements OnInit, OnDestroy {
  protected relationshipService: RelationshipService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);
  private route: ActivatedRoute = inject(ActivatedRoute);

  activeTab = signal<string>(this.route.snapshot.queryParamMap.get('tab') ?? 'friends');
  showRequestsDialog = signal<boolean>(false);
  showSearchDialog = signal<boolean>(false);
  searchQuery = signal<string>('');
  searchSource = signal<'friends' | 'followed'>('friends');

  friendSearch = signal<string>('');
  followedSearch = signal<string>('');
  blockedSearch = signal<string>('');

  pendingCount = computed(() => this.relationshipService.pendingRequestCount());

  private _destroy$ = new Subject<void>();

  // Debounced search subjects
  private friendSearchInput$ = new Subject<string>();
  private followedSearchInput$ = new Subject<string>();
  private blockedSearchInput$ = new Subject<string>();

  ngOnInit() {
    this.loadInitialData();

    this.route.queryParamMap.pipe(takeUntil(this._destroy$)).subscribe(params => {
      const tab = params.get('tab');
      if (tab === 'friends' || tab === 'followed' || tab === 'blocked') {
        this.activeTab.set(tab);
      }
    });

    this.eventBus.state$.pipe(takeUntil(this._destroy$)).subscribe((state) => {
    });

    // Setup debounced friend search
    this.friendSearchInput$
      .pipe(takeUntil(this._destroy$), debounceTime(300), distinctUntilChanged())
      .subscribe((query) => {
        this.performFriendSearch(query);
      });

    // Setup debounced followed search
    this.followedSearchInput$
      .pipe(takeUntil(this._destroy$), debounceTime(300), distinctUntilChanged())
      .subscribe((query) => {
        this.performFollowedSearch(query);
      });

    // Setup debounced blocked search
    this.blockedSearchInput$
      .pipe(takeUntil(this._destroy$), debounceTime(300), distinctUntilChanged())
      .subscribe((query) => {
        this.performBlockedSearch(query);
      });
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
    this.friendSearchInput$.complete();
    this.followedSearchInput$.complete();
    this.blockedSearchInput$.complete();
  }

  private loadInitialData() {
    this.relationshipService.loadFriends().pipe(takeUntil(this._destroy$)).subscribe();
    this.relationshipService.loadFollowed().pipe(takeUntil(this._destroy$)).subscribe();
    this.relationshipService.loadBlocked().pipe(takeUntil(this._destroy$)).subscribe();
    this.relationshipService.loadPendingRequestCount().pipe(takeUntil(this._destroy$)).subscribe();
  }

  private performFriendSearch(query: string) {
    this.relationshipService
      .loadFriends(query || undefined)
      .pipe(takeUntil(this._destroy$))
      .subscribe();
  }

  private performFollowedSearch(query: string) {
    this.relationshipService
      .loadFollowed(query || undefined)
      .pipe(takeUntil(this._destroy$))
      .subscribe();
  }

  private performBlockedSearch(query: string) {
    this.relationshipService
      .loadBlocked(query || undefined)
      .pipe(takeUntil(this._destroy$))
      .subscribe();
  }

  onFriendSearchChange(query: string) {
    this.friendSearch.set(query);
    this.friendSearchInput$.next(query);
  }

  onBlockedSearchChange(query: string) {
    this.blockedSearch.set(query);
    this.blockedSearchInput$.next(query);
  }

  onFollowedSearchChange(query: string) {
    this.followedSearch.set(query);
    this.followedSearchInput$.next(query);
  }

  openRequestsDialog() {
    this.showRequestsDialog.set(true);
    this.eventBus.requestRefresh();
  }

  closeRequestsDialog() {
    this.showRequestsDialog.set(false);
  }

  openSearchDialog(fromTab: 'friends' | 'followed') {
    this.searchSource.set(fromTab);
    this.showSearchDialog.set(true);
  }

  closeSearchDialog() {
    this.showSearchDialog.set(false);
    // Note: Not clearing search results so they persist when reopening
    this.loadInitialData();
  }
}

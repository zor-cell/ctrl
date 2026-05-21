import { Component, inject, signal, OnInit, computed, OnDestroy, Injector, afterNextRender } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom, forkJoin, from, Observable, Subject, switchMap, takeUntil } from 'rxjs';

import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { MultiSelectModule, MultiSelectFilterEvent } from 'primeng/multiselect';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { SearchService } from '../../services/search.service';
import { TopicService } from '../../../algorithm/topic.service';
import { NotificationService } from '../../../core/services/notification.service';
import { MediaCacheService } from '../../../core/services/media-cache.service';
import { PostPreview } from '../../dto/PostPreview';
import { TopicResponse } from '../../../post/dto/TopicResponse';
import { PostPreviewCardComponent } from '../post-preview-card/post-preview-card.component';

/**
 * Posts Search Component
 *
 * Features:
 * - Search by text query (3+ characters) - triggers on button click or Enter
 * - Topic multi-select filter (OR logic)
 * - Results displayed in a grid preview layout (similar to profile page)
 * - Clicking a post navigates to /posts/{postId} detail page
 * - Sticky search bar that stays at top when scrolling
 */
@Component({
  selector: 'app-posts-search',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    InputTextModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    MultiSelectModule,
    ProgressSpinnerModule,
    PostPreviewCardComponent,
  ],
  templateUrl: './posts-search.component.html',
  styleUrl: './posts-search.component.scss',
})

export class PostsSearchComponent implements OnInit, OnDestroy {
  private searchService = inject(SearchService);
  private topicService = inject(TopicService);
  private notificationService = inject(NotificationService);
  private mediaCacheService = inject(MediaCacheService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private injector = inject(Injector);

  private _destroy$ = new Subject<void>();

  // Search state
  searchQuery = signal<string>('');
  selectedTopics = signal<string[]>([]);
  availableTopics = signal<TopicResponse[]>([]);
  filterQuery = signal<string>('');

  // Sorted topics with selected ones at the top
  sortedTopics = computed(() => {
    const all = this.availableTopics();
    const selected = new Set(this.selectedTopics());
    const query = this.filterQuery().trim().toLowerCase();

    // Sort: selected first, then alphabetically
    let sorted = [...all].sort((a, b) => {
      const aSelected = selected.has(a.topicId);
      const bSelected = selected.has(b.topicId);

      if (aSelected && !bSelected) return -1;
      if (!aSelected && bSelected) return 1;
      return a.name.localeCompare(b.name);
    });

    // If there's a filter query, filter and score by match quality
    if (query) {
      sorted = sorted
        .map(t => ({ topic: t, score: this.scoreMatch(t.name, query) }))
        .filter(x => x.score > 0 || selected.has(x.topic.topicId))
        .sort((a, b) => {
          const aSelected = selected.has(a.topic.topicId);
          const bSelected = selected.has(b.topic.topicId);
          if (aSelected && !bSelected) return -1;
          if (!aSelected && bSelected) return 1;
          return b.score - a.score;
        })
        .map(x => x.topic);
    }

    return sorted;
  });

  // Results state
  searchResults = signal<PostPreview[]>([]);
  isSearching = signal<boolean>(false);
  hasSearched = signal<boolean>(false);

  // Pagination
  currentPage = signal<number>(0);
  hasMoreResults = signal<boolean>(true);
  private readonly postsPerPage = 21;

  ngOnInit(): void {
    // Data is resolved by the route resolver BEFORE component renders
    // This prevents any UI flicker of empty state
    const resolvedData = this.route.snapshot.data['searchData'];

    if (resolvedData) {
      // Initialize from resolved data (cache or URL params)
      this.searchQuery.set(resolvedData.query);
      this.selectedTopics.set(resolvedData.topics);

      if (resolvedData.query || resolvedData.topics.length > 0) {
        this.hasSearched.set(true);

        if (resolvedData.fromCache && resolvedData.results) {
          this.searchResults.set(resolvedData.results);
          this.hasMoreResults.set(resolvedData.results.length === this.postsPerPage);
          // Restore scroll position after DOM update
          if (resolvedData.scrollY > 0) {
            afterNextRender(() => {
              window.scrollTo({ top: resolvedData.scrollY, behavior: 'auto' });
            }, { injector: this.injector });
          }
        }
      }
    }

    // Load available topics
    this.topicService.getTopics().subscribe({
      next: (topics) => {
        this.availableTopics.set(topics);
      },
      error: () => {
        this.notificationService.showError('Failed to load topics');
      },
    });
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
  }

  /**
   * Update URL with current search state
   */
  private updateUrlState(): void {
    const queryParams: Record<string, string> = {};

    if (this.searchQuery()) {
      queryParams['q'] = this.searchQuery();
    }

    if (this.selectedTopics().length > 0) {
      queryParams['topics'] = this.selectedTopics().join(',');
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: Object.keys(queryParams).length > 0 ? queryParams : undefined,
      replaceUrl: true
    });
  }

  onSearchInputChange(query: string): void {
    this.searchQuery.set(query);
    // Debounce URL update for query changes (don't search yet, just update URL)
    if (this.searchQuery().length >= 3 || this.searchQuery().length === 0) {
      this.updateUrlState();
    }
  }

  onTopicFilter(event: MultiSelectFilterEvent): void {
    this.filterQuery.set(event.filter ?? '');
  }

  /**
   * Handle topic selection changes - update URL state
   */
  onTopicsChange(topics: string[]): void {
    this.selectedTopics.set(topics);
    this.currentPage.set(0);
    this.updateUrlState();
  }

  clearSearch(inputElement?: HTMLInputElement): void {
    this.searchQuery.set('');
    this.selectedTopics.set([]);
    this.searchResults.set([]);
    this.hasSearched.set(false);
    this.currentPage.set(0);
    this.hasMoreResults.set(true);

    // Clear cache and URL parameters
    this.searchService.clearPostSearchCache();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: undefined,
      replaceUrl: true
    });

    if (inputElement) {
      setTimeout(() => inputElement.focus(), 0);
    }
  }

  canSearch(): boolean {
    const hasQuery = this.searchQuery().trim().length >= 3;
    const hasTopics = this.selectedTopics().length > 0;
    return hasQuery || hasTopics;
  }

  onSearch(): void {
    if (!this.canSearch()) {
      this.notificationService.showError('Please enter at least 3 characters or select at least one topic');
      return;
    }

    // Reset pagination
    this.currentPage.set(0);
    this.hasMoreResults.set(true);
    window.scrollTo({ top: 0, behavior: 'auto' });

    // Update URL with search state
    this.updateUrlState();
    this.performSearch();
  }

  private performSearch(): void {
    this.searchResults.set([]);
    this.isSearching.set(true);
    this.hasSearched.set(true);

    const request = {
      query: this.searchQuery().trim(),
      topicIds: this.selectedTopics(),
      page: this.currentPage(),
      postsPerPage: this.postsPerPage,
    };

    this.searchService.searchPosts(request)
      .pipe(
        takeUntil(this._destroy$),
        switchMap(results => this.handleSearchResults(results))
      )
      .subscribe({
        next: results => {
          if (this.currentPage() === 0) {
            this.searchResults.set(results);
          } else {
            this.searchResults.update(current => [...current, ...results]);
          }
          this.hasMoreResults.set(results.length === this.postsPerPage);
          this.isSearching.set(false);
        },
        error: err => {
          this.notificationService.showError(err);
          this.isSearching.set(false);
        }
      });
  }

  private handleSearchResults(results: PostPreview[]): Observable<PostPreview[]> {
    return forkJoin(results.map(post => from(this.resolveSearchMedia(post))));
  }

  /**
   * Resolve media URLs (images/videos) for search results.
   * Processes videoImageRef and userProfilePic through the media cache.
   */
  private async resolveSearchMedia(post: PostPreview): Promise<PostPreview> {
    if (post.userProfilePic) {
      post.userProfilePic = await this.resolveMedia(
        post.userProfilePic,
        'image',
        'normal'
      ) ?? '';
    }

    if (post.mediaUrl) {
      post.mediaUrl = await this.resolveMedia(post.mediaUrl, this.mediaKind(post.mediaUrl), 'high');
    }
    return post;
  }

  private async resolveMedia(
    mediaUrl: string | null | undefined,
    kind: 'image' | 'video',
    priority: 'normal' | 'high'
  ): Promise<string | null> {
    return firstValueFrom(
      this.mediaCacheService.resolveMediaUrl(mediaUrl, kind, priority)
    ).catch(() => null);
  }

  private mediaKind(mediaUrl: string): 'image' | 'video' {
    return /\.(mp4|webm|mov)(\?|#|$)/i.test(mediaUrl) ? 'video' : 'image';
  }

  loadMore(): void {
    if (this.isSearching() || !this.hasMoreResults()) {
      return;
    }
    this.currentPage.update(page => page + 1);
    this.updateUrlState();
    this.performSearch();
  }

  /**
   * Handle post selection from the preview grid - navigate to post detail page
   * Saves current scroll position in search service cache
   */
  onPostSelected(preview: PostPreview): void {
    this.searchService.saveScrollPosition(window.scrollY);
    this.router.navigate(['/posts', preview.postId]);
  }

  /**
   * Check if a topic is selected
   */
  isTopicSelected(topicId: string): boolean {
    return this.selectedTopics().includes(topicId);
  }

  /**
   * Get display name for a topic by ID
   */
  getTopicDisplayName(topicId: string): string {
    const topic = this.availableTopics().find(t => t.topicId === topicId);
    return topic ? topic.name : topicId;
  }

  /**
   * Scores how well `name` matches `query`.
   *   3 – exact match
   *   2 – starts with query
   *   1 – contains query
   *   0 – no match
   */
  private scoreMatch(name: string, query: string): number {
    const n = name.toLowerCase();
    const q = query.toLowerCase();
    if (n === q) return 3;
    if (n.startsWith(q)) return 2;
    if (n.includes(q)) return 1;
    return 0;
  }
}

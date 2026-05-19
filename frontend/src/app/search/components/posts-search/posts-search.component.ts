import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil} from 'rxjs';

import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { MultiSelectModule, MultiSelectFilterEvent } from 'primeng/multiselect';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { CardModule } from 'primeng/card';
import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';

import { SearchService } from '../../services/search.service';
import { TopicService } from '../../../algorithm/topic.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Post } from '../../../feed/dto/Post';
import { TopicResponse } from '../../../post/dto/TopicResponse';
import { PostService, VoteType } from '../../../post/post.service';
import { CommentsComponent } from '../../../feed/components/comments/comments.component';

/**
 * Posts Search Component
 *
 * Features:
 * - Search by text query (3+ characters) - triggers on button click or Enter
 * - Topic multi-select filter (OR logic)
 * - Algorithm config button (opens config dialog)
 * - Loading spinner during search (embedding takes time)
 * - Results displayed as feed posts
 * - Empty state message when no search performed
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
    CardModule,
    AvatarModule,
    TooltipModule,
    TagModule,
    DialogModule,
    CommentsComponent,
  ],
  templateUrl: './posts-search.component.html',
  styleUrl: './posts-search.component.scss',
})
export class PostsSearchComponent implements OnInit {
  private searchService = inject(SearchService);
  private topicService = inject(TopicService);
  private notificationService = inject(NotificationService);
  private postService = inject(PostService);

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
        .filter(x => x.score > 0 || selected.has(x.topic.topicId)) // Keep selected even if not matching
        .sort((a, b) => {
          // Selected items always at the top
          const aSelected = selected.has(a.topic.topicId);
          const bSelected = selected.has(b.topic.topicId);
          if (aSelected && !bSelected) return -1;
          if (!aSelected && bSelected) return 1;
          // Then by match score
          return b.score - a.score;
        })
        .map(x => x.topic);
    }

    return sorted;
  });

  // Results state
  searchResults = signal<Post[]>([]);
  isSearching = signal<boolean>(false);
  hasSearched = signal<boolean>(false);

  // Infinite scroll / pagination
  currentPage = signal<number>(0);
  hasMoreResults = signal<boolean>(true);
  private readonly postsPerPage = 20;

  // Comments dialog state
  commentsDialogVisible = signal<boolean>(false);
  selectedPostForComments = signal<Post | null>(null);

  private _destroy$ = new Subject<void>();

  ngOnInit(): void {
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

  onSearchInputChange(query: string): void {
    this.searchQuery.set(query);
  }

  onTopicFilter(event: MultiSelectFilterEvent): void {
    this.filterQuery.set(event.filter ?? '');
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.selectedTopics.set([]);
    this.searchResults.set([]);
    this.hasSearched.set(false);
    this.currentPage.set(0);
    this.hasMoreResults.set(true);
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
    this.performSearch();
  }

  private performSearch(): void {
    this.isSearching.set(true);
    this.hasSearched.set(true);

    const request = {
      query: this.searchQuery().trim(),
      topicIds: this.selectedTopics(),
      page: this.currentPage(),
      postsPerPage: this.postsPerPage,
    };

    this.searchService
      .searchPosts(request)
      .pipe(takeUntil(this._destroy$))
      .subscribe({
        next: (results) => {
          if (this.currentPage() === 0) {
            this.searchResults.set(results);
          } else {
            this.searchResults.update(current => [...current, ...results]);
          }
          this.hasMoreResults.set(results.length === this.postsPerPage);
          this.isSearching.set(false);
        },
        error: (error) => {
          this.notificationService.showError('Search failed. Please try again.');
          this.isSearching.set(false);
          console.error('Search error:', error);
        },
      });
  }

  loadMore(): void {
    if (this.isSearching() || !this.hasMoreResults()) {
      return;
    }
    this.currentPage.update(page => page + 1);
    this.performSearch();
  }

  // Post interaction methods
  async votePost(post: Post, voteType: VoteType): Promise<void> {
    const previousVote = post.userVote;
    const previousUpvotes = post.upvotes;
    const previousDownvotes = post.downvotes;

    // Optimistic update
    const isSameVote = post.userVote === voteType;
    const optimistic = this.calcOptimisticVote(post.upvotes, post.downvotes, post.userVote, voteType, isSameVote);
    this.patchPost(post.postId, optimistic);

    try {
      const result = await this.postService.votePost(post.postId, voteType);
      this.patchPost(post.postId, {
        upvotes: result.upvotes,
        downvotes: result.downvotes,
        userVote: result.userVote,
      });
    } catch (err) {
      // Revert on error
      this.patchPost(post.postId, {
        upvotes: previousUpvotes,
        downvotes: previousDownvotes,
        userVote: previousVote,
      });
      this.notificationService.showError('Failed to vote on post');
    }
  }

  async voteSource(post: Post, voteType: VoteType): Promise<void> {
    const source = post.sourceIds?.[0];
    if (!source?.sourceId) return;

    const previousVote = source.userVote;
    const previousUpvotes = source.upvotes;
    const previousDownvotes = source.downvotes;

    const isSameVote = source.userVote === voteType;
    const optimistic = this.calcOptimisticVote(source.upvotes, source.downvotes, source.userVote, voteType, isSameVote);
    this.patchSource(post.postId, optimistic);

    try {
      const result = await this.postService.voteSource(source.sourceId, voteType);
      this.patchSource(post.postId, {
        upvotes: result.upvotes,
        downvotes: result.downvotes,
        userVote: result.userVote,
      });
    } catch (err) {
      this.patchSource(post.postId, {
        upvotes: previousUpvotes,
        downvotes: previousDownvotes,
        userVote: previousVote,
      });
      this.notificationService.showError('Failed to vote on source');
    }
  }

  openCommentsDialog(post: Post): void {
    this.selectedPostForComments.set(post);
    this.commentsDialogVisible.set(true);
  }

  // Helper methods
  private calcOptimisticVote(
    upvotes: number,
    downvotes: number,
    currentVote: 'up' | 'down' | null | undefined,
    voteType: VoteType,
    isSameVote: boolean
  ): { upvotes: number; downvotes: number; userVote: VoteType | null } {
    if (isSameVote) {
      if (voteType === 'up') upvotes = Math.max(0, upvotes - 1);
      else downvotes = Math.max(0, downvotes - 1);
      return { upvotes, downvotes, userVote: null };
    }
    if (voteType === 'up') {
      upvotes += 1;
      if (currentVote === 'down') downvotes = Math.max(0, downvotes - 1);
    } else {
      downvotes += 1;
      if (currentVote === 'up') upvotes = Math.max(0, upvotes - 1);
    }
    return { upvotes, downvotes, userVote: voteType };
  }

  private patchPost(postId: string, patch: Partial<Post>): void {
    this.searchResults.update(posts =>
      posts.map(p => (p.postId === postId ? { ...p, ...patch } : p))
    );
  }

  private patchSource(postId: string, patch: Partial<Post['sourceIds'][0]>): void {
    this.searchResults.update(posts =>
      posts.map(p => {
        if (p.postId !== postId || !p.sourceIds?.length) return p;
        const updatedSources = p.sourceIds.map((s, i) =>
          i === 0 ? { ...s, ...patch } : s
        );
        return { ...p, sourceIds: updatedSources };
      })
    );
  }

  getDomain(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
  }

  timeAgo(createdAt: string): string {
    const normalized = createdAt.endsWith('Z') ? createdAt : createdAt + 'Z';
    const diffMs = Date.now() - new Date(normalized).getTime();
    const minutes = Math.floor(diffMs / 60_000);
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  }

  isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)$/i.test(url);
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
    return topic ? `${topic.name}` : topicId;
  }

  /**
   * Scores how well `name` matches `query` (the query must already be lower-cased).
   *   3 – exact match
   *   2 – starts with query
   *   1 – contains query (anywhere)
   *   0 – no match
   */
  private scoreMatch(name: string, query: string): number {
    const n = name.toLowerCase();
    if (n === query) return 3;
    if (n.startsWith(query)) return 2;
    if (n.includes(query)) return 1;
    return 0;
  }
}

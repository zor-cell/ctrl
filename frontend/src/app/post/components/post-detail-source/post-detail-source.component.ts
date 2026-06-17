import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  inject,
  Input,
  Output,
  signal,
} from '@angular/core';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { PostService, VoteType } from '../../post.service';
import { PostStateService } from '../../services/post-state.service';
import { SourceResponse } from '../../dto/PostResponse';

@Component({
  selector: 'app-post-detail-source',
  standalone: true,
  imports: [TagModule, TooltipModule],
  templateUrl: './post-detail-source.component.html',
  styleUrl: './post-detail-source.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostDetailSourceComponent {
  private readonly postService = inject(PostService);
  private readonly postState   = inject(PostStateService);
  private readonly cdr         = inject(ChangeDetectorRef);

  /** Full SourceResponse passed down from the parent */
  @Input() source: SourceResponse | null | undefined = null;

  /** postId needed to write votes back into the shared store */
  @Input() postId: string | null = null;

  /** Current source-vote, derived from the shared store in the parent */
  @Input() sourceVote: 'up' | 'down' | null = null;

  @Output() readonly reportClicked = new EventEmitter<void>();

  /** Emits the resolved vote so the parent's computed signal stays in sync */
  @Output() readonly voteChanged = new EventEmitter<'up' | 'down' | null>();

  // ─── Collapse state ───────────────────────────────────────────────────────

  protected readonly collapsed = signal(true);

  protected toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  // ─── Voting ───────────────────────────────────────────────────────────────

  private votingInProgress = false;

  protected async vote(voteType: VoteType): Promise<void> {
    const sourceId = this.source?.sourceId;
    if (!sourceId || !this.postId || this.votingInProgress) return;
    this.votingInProgress = true;

    const prevVote   = this.sourceVote;
    const isSameVote = prevVote === voteType;
    const optimistic = isSameVote ? null : voteType;

    this.postState.setSourceVote(this.postId, optimistic);
    this.voteChanged.emit(optimistic);
    this.cdr.markForCheck();

    try {
      const result   = await this.postService.voteSource(sourceId, voteType);
      const resolved = result.userVote ?? null;
      this.postState.setSourceVote(this.postId, resolved);
      this.voteChanged.emit(resolved);
    } catch (err) {
      console.error('Source-vote failed; reverting:', err);
      this.postState.setSourceVote(this.postId, prevVote);
      this.voteChanged.emit(prevVote);
    } finally {
      this.votingInProgress = false;
      this.cdr.markForCheck();
    }
  }

  // ─── Utilities ────────────────────────────────────────────────────────────

  protected getDomain(url: string | null): string {
    if (!url) return '';
    try   { return new URL(url).hostname.replace(/^www\./, ''); }
    catch { return url; }
  }

  protected formatDate(date: string | null): string {
    if (!date) return '';
    try {
      const d = new Date(date.includes('T') ? date : date + 'T00:00:00');
      return d.toLocaleDateString('de-AT', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch {
      return date;
    }
  }
}

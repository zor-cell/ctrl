import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  signal,
  SimpleChanges,
} from '@angular/core';
import { DialogModule } from 'primeng/dialog';
import { PostDetailViewComponent } from "../post-detail-view/post-detail-view.component";
import { ReportComponent } from '../../../report/components/report/report.component';
import { ContentTypeEnum } from '../../../report/dto/report';
import { CommentResponse } from '../../dto/PostResponse';

@Component({
  selector: 'app-post-detail-dialog',
  standalone: true,
  imports: [DialogModule, PostDetailViewComponent, ReportComponent],
  templateUrl: './post-detail-dialog.component.html',
  styleUrl: './post-detail-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostDetailDialogComponent implements OnChanges {
  @Input() postId: string | null = null;
  @Output() readonly dismissed   = new EventEmitter<void>();
  @Output() readonly postDeleted = new EventEmitter<string>();

  protected readonly dialogVisible = signal(false);

  private readonly cdr = inject(ChangeDetectorRef);

  // ─── Report ───────────────────────────────────────────────────────────────
  protected readonly ContentTypeEnum = ContentTypeEnum;

  protected readonly reportPostData    = signal<{ postId: string; caption: string; username: string } | null>(null);
  protected readonly reportSourceData  = signal<{ sourceId: string | undefined; url: string | undefined; username: string } | null>(null);
  protected readonly reportCommentData = signal<CommentResponse | null>(null);

  protected readonly reportPostVisible    = signal(false);
  protected readonly reportSourceVisible  = signal(false);
  protected readonly reportCommentVisible = signal(false);

  // ─── Lifecycle ────────────────────────────────────────────────────────────

  ngOnChanges(changes: SimpleChanges): void {
    const change = changes['postId'];
    if (change?.currentValue) {
      this.dialogVisible.set(true);
    } else if (change && !change.currentValue) {
      this.dialogVisible.set(false);
    }
  }

  // ─── Dialog ───────────────────────────────────────────────────────────────

  protected onVisibleChange(visible: boolean): void {
    if (!visible) {
      this.dialogVisible.set(false);
      this.dismissed.emit();
    }
  }

  protected onPostDeleted(postId: string): void {
    this.dialogVisible.set(false);
    this.postDeleted.emit(postId);
  }

  // ─── Report ───────────────────────────────────────────────────────────────

  protected onReportPost(data: { postId: string; caption: string; username: string }): void {
    this.reportPostData.set(data);
    this.reportPostVisible.set(true);
    this.cdr.markForCheck();
  }

  protected onReportSource(data: { sourceId: string | undefined; url: string | undefined; username: string }): void {
    this.reportSourceData.set(data);
    this.reportSourceVisible.set(true);
    this.cdr.markForCheck();
  }

  protected onReportComment(comment: CommentResponse): void {
    this.reportCommentData.set(comment);
    this.reportCommentVisible.set(true);
    this.cdr.markForCheck();
  }
}

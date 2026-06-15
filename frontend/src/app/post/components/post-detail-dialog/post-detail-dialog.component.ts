import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  signal,
  SimpleChanges,
} from '@angular/core';
import { DialogModule } from 'primeng/dialog';
import { PostDetailViewComponent } from "../post-detail-view/post-detail-view.component";

@Component({
  selector: 'app-post-detail-dialog',
  standalone: true,
  imports: [DialogModule, PostDetailViewComponent],
  templateUrl: './post-detail-dialog.component.html',
  styleUrl: './post-detail-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostDetailDialogComponent implements OnChanges {
  @Input() postId: string | null = null;
  @Output() readonly dismissed   = new EventEmitter<void>();
  @Output() readonly postDeleted = new EventEmitter<string>();

  protected readonly dialogVisible = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    const change = changes['postId'];
    if (change?.currentValue) {
      this.dialogVisible.set(true);
    } else if (change && !change.currentValue) {
      this.dialogVisible.set(false);
    }
  }

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
}

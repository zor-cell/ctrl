import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AvatarModule } from 'primeng/avatar';
import { PostPreview } from '../../dto/PostPreview';

/**
 * Post Preview Card Component
 *
 * A reusable single post card component that displays:
 * - User avatar and username (top left)
 * - Post media (if available) or full caption
 * - Truncated caption (3 lines) when media is present
 *
 * Used in grid layouts for post previews. Clicking the card emits an open event.
 */
@Component({
  selector: 'app-post-preview-card',
  standalone: true,
  imports: [CommonModule, AvatarModule],
  templateUrl: './post-preview-card.component.html',
  styleUrl: './post-preview-card.component.scss',
})
export class PostPreviewCardComponent {
  /** The post preview data to display */
  post = input.required<PostPreview>();

  /** Event emitted when the card is clicked to open the post detail */
  open = output<void>();

  /**
   * Check if a URL points to a video file
   */
  isVideo(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)$/i.test(url);
  }

  /**
   * Handle card click
   */
  onClick(): void {
    this.open.emit();
  }
}

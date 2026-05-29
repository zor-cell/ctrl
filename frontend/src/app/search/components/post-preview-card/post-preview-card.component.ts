import { Component, inject, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AvatarModule } from 'primeng/avatar';
import { PostPreview } from '../../dto/PostPreview';
import { Router } from '@angular/router';
import { MediaCacheService } from '../../../core/services/media-cache.service';
import { rxResource } from '@angular/core/rxjs-interop';
import { tap } from 'rxjs';

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
  private readonly mediaService = inject(MediaCacheService);
  private readonly router = inject(Router);
  /** The post preview data to display */
  post = input.required<PostPreview>();
  profilePic = rxResource({
    params: () => {
      const val = this.post().userProfilePicture || undefined;
      console.log('profilePic param:', val);
      return val;
    },
    stream: ({ params }) => this.mediaService.resolveMediaUrl(params).pipe(
      tap(url => console.log('profilePic resolved:', url))
    )
  });
  mediaRef = rxResource({
    params: () => this.post().mediaUrl,
    stream: ({ params }) => {
      return this.mediaService.resolveMediaUrl(params)
    }
  })

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

  goToUser(): void {
    this.router.navigate(['/profile', this.post().username]);
  }
}

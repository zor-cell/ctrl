import { CommonModule, Location } from '@angular/common';
import { Component, inject, resource, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';

import { timeAgo as formatTimeAgo } from '../../../core/utils/date-time.utils';
import { PostService } from '../../post.service';
import { DetailedPostResponse } from '../../dto/DetailedPostResponse';
import { PostComments } from '../post-comments/post-comments';
import { PostActions } from '../post-actions/post-actions-component';
import { ContentTypeEnum } from '../../../report/dto/report';
import { PostSources } from '../post-sources/post-sources-component';
import { MediaCacheService } from '../../../core/services/media-cache.service';
import { rxResource } from '@angular/core/rxjs-interop';
import {Tag} from 'primeng/tag';


@Component({
  selector: 'app-post-detail-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    AvatarModule,
    CardModule,
    ProgressSpinnerModule,
    TooltipModule,
    PostComments,
    PostActions,
    PostSources,
    Tag,
  ],
  templateUrl: './post-detail-page.component.html',
  styleUrl: './post-detail-page.component.scss',
})
export class PostDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly postService = inject(PostService);
  private readonly mediaService = inject(MediaCacheService);

  protected ContentTypeEnum = ContentTypeEnum;

  postId = this.route.snapshot.paramMap.get('postId');
  private readonly returnChatId = this.route.snapshot.queryParamMap.get('fromChat');
  post = resource({
    loader: () => {
      return this.postService.getPostById(this.postId!);
    },
  });
  profilePic = rxResource({
    params: () => this.post.value()?.userProfilePicture ?? undefined,
    stream: ({ params }) => {
      return this.mediaService.resolveMediaUrl(params);
    },
  });
  mediaRef = rxResource({
    params: () => this.post.value()?.mediaUrl,
    stream: ({ params }) => {
      return this.mediaService.resolveMediaUrl(params);
    },
  });
  commentsVisible = signal<boolean>(false);

  goBack(): void {
    if (this.returnChatId) {
      this.router.navigate(['/chat'], { queryParams: { chat: this.returnChatId } });
      return;
    }

    this.location.back();
  }

  errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const backendMessage = this.extractBackendErrorMessage(error.error);
      return backendMessage ?? error.message ?? 'Post could not be loaded.';
    }

    if (error instanceof Error) {
      return error.message;
    }

    if (typeof error === 'string') {
      return error;
    }

    return 'Post could not be loaded.';
  }

  goToUser(): void {
    const post = this.post.value();
    if (!post || post.userDeleted) {
      return;
    }

    if (post.isExternal) {
      if (post.userProfileUrl) {
        window.open(post.userProfileUrl);
      }
    } else {
      this.router.navigate(['/profile', post.username]);
    }
  }

  // Helper methods
  postMediaKind(post: DetailedPostResponse): 'image' | 'video' | null {
    if (post.mediaUrl == null) {
      return null;
    }
    if (/\.(mp4|webm|ogg|mov)(?:[?#].*)?$/i.test(post.mediaUrl)) {
      return 'video';
    } else {
      return 'image';
    }
  }

  timeAgo(value: string): string {
    return formatTimeAgo(value);
  }

  private extractBackendErrorMessage(error: unknown): string | null {
    if (typeof error === 'string') {
      return error;
    }

    if (error && typeof error === 'object') {
      const candidate = error as { message?: unknown; error?: unknown; detail?: unknown };
      const message = candidate.message ?? candidate.error ?? candidate.detail;
      return typeof message === 'string' && message.trim() ? message : null;
    }

    return null;
  }
}

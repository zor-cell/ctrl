import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
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
  ],
  templateUrl: './post-detail-page.component.html',
  styleUrl: './post-detail-page.component.scss',
})
export class PostDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly postService = inject(PostService);

  protected ContentTypeEnum = ContentTypeEnum;

  // Post data
  post = signal<DetailedPostResponse | null>(null);
  isLoading = signal<boolean>(true);
  error = signal<string | null>(null);
  commentsVisible = signal<boolean>(false);

  ngOnInit(): void {
    const postId = this.route.snapshot.paramMap.get('postId');
    if (!postId) {
      this.error.set('No post ID provided');
      this.isLoading.set(false);
      return;
    }

    this.loadPost(postId);
  }

  private async loadPost(postId: string): Promise<void> {
    this.isLoading.set(true);
    try {
      const post = await this.postService.getPostById(postId);
      this.post.set(post);
    } catch (error) {
      this.error.set('Failed to load post');
      console.error('Failed to load post:', error);
    } finally {
      this.isLoading.set(false);
    }
  }

  goBack(): void {
    this.location.back();
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

  postMediaUrl(post: DetailedPostResponse): string | null {
    return post.mediaUrl;
  }

  timeAgo(value: string): string {
    return formatTimeAgo(value);
  }
}

import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';

import { NotificationService } from '../core/services/notification.service';
import { UserService } from '../user/user.service';
import { PostResponse } from './dto/PostResponse';
import { TopicResponse } from './dto/TopicResponse';
import { PostComposerComponent } from './post-composer.component';
import { PostDetailDialogComponent } from './post-detail-dialog.component';
import { postMediaKind } from './post-media.utils';
import { PreviewKind, TopicOption } from './post-page.models';
import { PostService } from './post.service';

@Component({
  selector: 'app-post-page',
  imports: [CommonModule, Button, Dialog, PostComposerComponent, PostDetailDialogComponent],
  templateUrl: './post-page.component.html',
  styleUrl: './post-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostPageComponent implements OnDestroy, OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly postService = inject(PostService);
  private readonly notificationService = inject(NotificationService);
  protected readonly userService = inject(UserService);
  private readonly avatarInput =
    viewChild<ElementRef<HTMLInputElement>>('avatarInput');

  protected readonly dialogVisible = signal(false);
  protected readonly avatarPreviewUrl = signal<string | null>(null);
  protected readonly myPosts = signal<PostResponse[]>([]);
  protected readonly selectedPost = signal<PostResponse | null>(null);
  protected readonly loadingPosts = signal(false);
  protected readonly updatingAvatar = signal(false);
  protected readonly editingBio = signal(false);
  protected readonly updatingBio = signal(false);
  protected readonly bioDraft = signal('');
  protected readonly deletingPost = signal(false);
  protected readonly postPendingDeletion = signal<PostResponse | null>(null);
  protected readonly loadingTopics = signal(false);
  protected readonly topicOptions = signal<TopicOption[]>([]);

  protected readonly username = computed(
    () => this.userService.user()?.username ?? 'Unknown user',
  );
  protected readonly avatarInitial = computed(
    () => this.username().trim().charAt(0).toUpperCase() || '?',
  );
  protected readonly currentProfilePicture = computed(
    () => this.avatarPreviewUrl() ?? this.userService.user()?.profilePicture ?? null,
  );
  protected readonly userBio = computed(() => {
    const bio = this.userService.user()?.bio?.trim();
    return bio && bio.length > 0 ? bio : 'No bio yet.';
  });
  protected readonly bioLength = computed(() => this.bioDraft().length);
  protected readonly bioTooLong = computed(() => this.bioLength() > this.maxBioLength);
  protected readonly postsCountLabel = computed(() => {
    const count = this.myPosts().length;
    return count === 1 ? '1 post' : `${count} posts`;
  });
  protected readonly isVerifiedUser = computed(
    () => this.userService.user()?.isVerified ?? false,
  );
  protected readonly maxBioLength = 280;
  protected readonly createOnlyMode = signal(false);

  ngOnInit() {
    this.createOnlyMode.set(this.route.snapshot.routeConfig?.path === 'post');
    this.loadTopics();
    if (!this.createOnlyMode()) {
      this.loadMyPosts();
    }

    if (this.createOnlyMode()) {
      this.openCreateDialog();
    }
  }

  ngOnDestroy() {
    this.revokeAvatarPreview();
  }

  protected openCreateDialog() {
    this.dialogVisible.set(true);
  }

  protected onPostCreated() {
    if (this.createOnlyMode()) {
      this.router.navigate(['/profile']);
      return;
    }

    this.loadMyPosts();
  }

  protected onComposerVisibleChange(visible: boolean) {
    this.dialogVisible.set(visible);

    if (!visible && this.createOnlyMode()) {
      this.router.navigate(['/profile']);
    }
  }

  protected openProfileSettings() {
    this.router.navigate(['/settings']);
  }

  protected startBioEdit() {
    this.bioDraft.set(this.userService.user()?.bio ?? '');
    this.editingBio.set(true);
  }

  protected cancelBioEdit() {
    if (this.updatingBio()) {
      return;
    }

    this.editingBio.set(false);
    this.bioDraft.set('');
  }

  protected updateBioDraft(event: Event) {
    const input = event.target as HTMLTextAreaElement;
    this.bioDraft.set(input.value);
  }

  protected saveBio() {
    const user = this.userService.user();
    if (!user) {
      return;
    }

    if (this.bioTooLong()) {
      this.notificationService.showWarning(
        `Bio must not be longer than ${this.maxBioLength} characters.`,
      );
      return;
    }

    this.updatingBio.set(true);
    this.userService.updateUser({
      name: user.name,
      email: user.email,
      bio: this.bioDraft().trim(),
      birthdate: user.birthdate,
    }).subscribe({
      next: () => {
        this.updatingBio.set(false);
        this.editingBio.set(false);
      },
      error: () => {
        this.updatingBio.set(false);
      },
    });
  }

  protected openAvatarPicker() {
    this.avatarInput()?.nativeElement.click();
  }

  protected onAvatarSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);

    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.notificationService.showWarning(
        'Please choose an image as profile picture.',
      );
      input.value = '';
      return;
    }

    this.revokeAvatarPreview();
    this.avatarPreviewUrl.set(URL.createObjectURL(file));
    this.updatingAvatar.set(true);
    this.userService.updateProfilePicture(file).subscribe({
      next: () => {
        this.updatingAvatar.set(false);
        this.revokeAvatarPreview();
      },
      error: () => {
        this.updatingAvatar.set(false);
      },
    });
  }

  protected topicLabelById(topicId: string): string {
    return (
      this.topicOptions().find(option => option.value === topicId)?.label ?? topicId
    );
  }

  protected openPost(post: PostResponse) {
    this.selectedPost.set(post);
  }

  protected closePost() {
    this.selectedPost.set(null);
  }

  protected requestPostDeletion(post: PostResponse) {
    this.postPendingDeletion.set(post);
  }

  protected cancelPostDeletion() {
    if (this.deletingPost()) {
      return;
    }

    this.postPendingDeletion.set(null);
  }

  protected confirmPostDeletion() {
    const post = this.postPendingDeletion();

    if (!post) {
      return;
    }

    this.deletingPost.set(true);
    this.postService.deletePost(post.postId).subscribe({
      next: () => {
        this.myPosts.update(posts =>
          posts.filter(currentPost => currentPost.postId !== post.postId),
        );

        this.closePost();

        this.postPendingDeletion.set(null);
        this.deletingPost.set(false);
        this.notificationService.showSuccess('Post deleted.');
      },
      error: () => {
        this.deletingPost.set(false);
      },
    });
  }

  protected postMediaUrl(post: PostResponse | null): string | null {
    if (!post) {
      return null;
    }

    return post.mediaUrl;
  }

  protected postMediaKind(post: PostResponse): PreviewKind {
    return postMediaKind(post.mediaRef ?? post.mediaUrl);
  }

  private loadMyPosts() {
    this.loadingPosts.set(true);
    this.postService.getMyPosts().subscribe({
      next: posts => {
        this.myPosts.set(posts);
        this.loadingPosts.set(false);
      },
      error: () => {
        this.myPosts.set([]);
        this.loadingPosts.set(false);
        this.notificationService.showWarning(
          'Your posts could not be loaded from the backend.',
          'Posts unavailable',
        );
      },
    });
  }

  private loadTopics() {
    this.loadingTopics.set(true);
    this.postService.getTopics().subscribe({
      next: topics => {
        this.topicOptions.set(topics.map(topic => this.toTopicOption(topic)));
        this.loadingTopics.set(false);
      },
      error: () => {
        this.topicOptions.set([]);
        this.loadingTopics.set(false);
        this.notificationService.showWarning(
          'Topics could not be loaded from the backend.',
          'Topics unavailable',
        );
      },
    });
  }

  private toTopicOption(topic: TopicResponse): TopicOption {
    return {
      label: `${topic.emoji} ${topic.name}`,
      value: topic.topicId,
    };
  }

  private revokeAvatarPreview() {
    const currentAvatarPreviewUrl = this.avatarPreviewUrl();
    if (currentAvatarPreviewUrl) {
      URL.revokeObjectURL(currentAvatarPreviewUrl);
      this.avatarPreviewUrl.set(null);
    }
  }
}

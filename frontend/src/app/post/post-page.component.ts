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
import { TooltipModule } from 'primeng/tooltip';
import { Subscription } from 'rxjs';

import { MediaCacheService } from '../core/services/media-cache.service';
import { NotificationService } from '../core/services/notification.service';
import { PublicUserProfile } from '../user/dto/PublicUserProfile';
import { UserService } from '../user/user.service';
import { USERNAME_PATTERN } from '../user/validators';
import { PostResponse } from './dto/PostResponse';
import { PostCaptionFormatPipe } from './post-caption-format.pipe';
import { TopicResponse } from './dto/TopicResponse';
import { PostComposerComponent } from './post-composer.component';
import { PostDetailDialogComponent } from './post-detail-dialog.component';
import { postMediaKind } from './post-media.utils';
import { PreviewKind, TopicOption } from './post-page.models';
import { PostService } from './post.service';

@Component({
  selector: 'app-post-page',
  imports: [
    CommonModule,
    Button,
    Dialog,
    TooltipModule,
    PostCaptionFormatPipe,
    PostComposerComponent,
    PostDetailDialogComponent,
  ],
  templateUrl: './post-page.component.html',
  styleUrl: './post-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostPageComponent implements OnDestroy, OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly postService = inject(PostService);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly notificationService = inject(NotificationService);
  protected readonly userService = inject(UserService);
  private readonly avatarInput =
    viewChild<ElementRef<HTMLInputElement>>('avatarInput');

  protected readonly dialogVisible = signal(false);
  protected readonly avatarPreviewUrl = signal<string | null>(null);
  protected readonly profilePictureObjectUrl = signal<string | null>(null);
  protected readonly postMediaObjectUrls = signal<Record<string, string>>({});
  protected readonly postThumbnailObjectUrls = signal<Record<string, string>>({});
  protected readonly myPosts = signal<PostResponse[]>([]);
  protected readonly selectedPost = signal<PostResponse | null>(null);
  protected readonly publicProfile = signal<PublicUserProfile | null>(null);
  protected readonly publicProfileUsername = signal<string | null>(null);
  protected readonly loadingPosts = signal(false);
  protected readonly updatingAvatar = signal(false);
  protected readonly editingProfile = signal(false);
  protected readonly updatingProfile = signal(false);
  protected readonly profileBioDraft = signal('');
  protected readonly profileUsernameDraft = signal('');
  protected readonly selectedProfilePicture = signal<File | null>(null);
  protected readonly removeProfilePicture = signal(false);
  protected readonly usernameAvailable = signal<boolean | null>(null);
  protected readonly checkingUsername = signal(false);
  protected readonly deletingPost = signal(false);
  protected readonly postPendingDeletion = signal<PostResponse | null>(null);
  protected readonly loadingTopics = signal(false);
  protected readonly topicOptions = signal<TopicOption[]>([]);

  protected readonly username = computed(
    () =>
      this.publicProfile()?.username
      ?? this.userService.user()?.username
      ?? 'Unknown user',
  );
  protected readonly avatarInitial = computed(
    () => this.username().trim().charAt(0).toUpperCase() || '?',
  );
  protected readonly currentProfilePicture = computed(
    () =>
      this.removeProfilePicture()
        ? null
        : this.avatarPreviewUrl() ?? this.profilePictureObjectUrl() ?? null,
  );
  protected readonly userBio = computed(() => {
    const bio = (this.publicProfile()?.bio ?? this.userService.user()?.bio)?.trim();
    return bio && bio.length > 0 ? bio : 'No bio yet.';
  });
  protected readonly publicProfileMode = computed(() => this.publicProfileUsername() !== null);
  protected readonly canEditProfile = computed(
    () => !this.createOnlyMode() && !this.publicProfileMode(),
  );
  protected readonly bioLength = computed(() => this.profileBioDraft().length);
  protected readonly bioTooLong = computed(() => this.bioLength() > this.maxBioLength);
  protected readonly usernameDraftTrimmed = computed(() => this.profileUsernameDraft().trim());
  protected readonly usernameChanged = computed(
    () => this.usernameDraftTrimmed() !== this.userService.user()?.username,
  );
  protected readonly usernameEmpty = computed(() => this.usernameDraftTrimmed().length === 0);
  protected readonly usernameInvalid = computed(
    () => !this.usernameEmpty()
      && (this.usernameDraftTrimmed().length < 3
        || !USERNAME_PATTERN.test(this.usernameDraftTrimmed())),
  );
  protected readonly canSaveProfile = computed(
    () =>
      !this.updatingProfile()
      && !this.bioTooLong()
      && !this.usernameEmpty()
      && !this.usernameInvalid()
      && !this.checkingUsername()
      && this.usernameAvailable() !== false,
  );
  protected readonly postsCountLabel = computed(() => {
    const count = this.myPosts().length;
    return count === 1 ? '1 post' : `${count} posts`;
  });
  protected readonly profileEyebrow = computed(() =>
    this.publicProfileMode() ? 'Profile' : 'Archive',
  );
  protected readonly postsHeading = computed(() =>
    this.publicProfileMode() ? `${this.username()}'s posts` : 'Your posts',
  );
  protected readonly privatePostsHidden = computed(
    () => this.publicProfileMode() && this.publicProfile()?.canSeePrivatePosts === false,
  );
  protected readonly emptyPostsTitle = computed(() =>
    this.privatePostsHidden() ? 'Private posts are hidden' : 'No posts yet',
  );
  protected readonly emptyPostsMessage = computed(() => {
    if (this.privatePostsHidden()) {
      return `Become friends with ${this.username()} to see private posts.`;
    }

    return this.publicProfileMode()
      ? 'Public posts will appear here.'
      : 'Your posts will appear here after you create them.';
  });
  protected readonly isVerifiedUser = computed(
    () => this.userService.user()?.isVerified ?? false,
  );
  protected readonly maxBioLength = 280;
  protected readonly createOnlyMode = signal(false);
  private usernameCheckTimeout: ReturnType<typeof setTimeout> | null = null;
  private routeParamsSubscription: Subscription | null = null;

  ngOnInit() {
    this.createOnlyMode.set(this.route.snapshot.routeConfig?.path === 'post');
    this.loadTopics();

    this.routeParamsSubscription = this.route.paramMap.subscribe(params => {
      this.loadProfileForRoute(params.get('username'));
    });

    if (this.createOnlyMode()) {
      this.openCreateDialog();
    }
  }

  ngOnDestroy() {
    this.routeParamsSubscription?.unsubscribe();
    if (this.usernameCheckTimeout) {
      clearTimeout(this.usernameCheckTimeout);
    }
    this.revokeAvatarPreview();
    this.revokeProfilePictureObjectUrl();
    this.revokePostMediaObjectUrls();
    this.revokePostThumbnailObjectUrls();
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

  protected startProfileEdit() {
    const user = this.userService.user();
    if (!user) {
      return;
    }

    this.profileUsernameDraft.set(user.username);
    this.profileBioDraft.set(user.bio ?? '');
    this.usernameAvailable.set(null);
    this.selectedProfilePicture.set(null);
    this.removeProfilePicture.set(false);
    this.editingProfile.set(true);
  }

  protected cancelProfileEdit() {
    if (this.updatingProfile()) {
      return;
    }

    this.editingProfile.set(false);
    this.profileBioDraft.set('');
    this.profileUsernameDraft.set('');
    this.usernameAvailable.set(null);
    this.selectedProfilePicture.set(null);
    this.removeProfilePicture.set(false);
    this.revokeAvatarPreview();
  }

  protected updateBioDraft(event: Event) {
    const input = event.target as HTMLTextAreaElement;
    this.profileBioDraft.set(input.value);
  }

  protected updateUsernameDraft(event: Event) {
    const input = event.target as HTMLInputElement;
    this.profileUsernameDraft.set(input.value);
    this.usernameAvailable.set(null);

    if (this.usernameCheckTimeout) {
      clearTimeout(this.usernameCheckTimeout);
    }

    if (this.usernameEmpty() || this.usernameInvalid() || !this.usernameChanged()) {
      this.checkingUsername.set(false);
      return;
    }

    this.checkingUsername.set(true);
    this.usernameCheckTimeout = setTimeout(() => {
      const username = this.usernameDraftTrimmed();
      this.userService.isUsernameAvailable(username).subscribe({
        next: available => {
          if (this.usernameDraftTrimmed() === username) {
            this.usernameAvailable.set(available);
            this.checkingUsername.set(false);
          }
        },
        error: () => {
          this.usernameAvailable.set(false);
          this.checkingUsername.set(false);
        },
      });
    }, 350);
  }

  protected saveProfileChanges() {
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

    if (this.usernameInvalid()) {
      this.notificationService.showWarning(
        'Username must be at least 3 characters and may only contain letters, numbers, underscores and dots.',
      );
      return;
    }

    if (!this.canSaveProfile()) {
      return;
    }

    this.updatingProfile.set(true);
    this.userService.updateUser({
      name: user.name,
      email: user.email,
      bio: this.profileBioDraft().trim(),
      birthdate: user.birthdate,
    }, false).subscribe({
      next: () => this.saveUsernameIfNeeded(),
      error: () => this.updatingProfile.set(false),
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
    this.selectedProfilePicture.set(file);
    this.removeProfilePicture.set(false);
  }

  protected resetProfilePicture() {
    if (this.updatingProfile()) {
      return;
    }

    this.revokeAvatarPreview();
    this.selectedProfilePicture.set(null);
    this.removeProfilePicture.set(Boolean(this.userService.user()?.profilePicture));
  }

  protected topicLabelById(topicId: string): string {
    return (
      this.topicOptions().find(option => option.value === topicId)?.label ?? topicId
    );
  }

  protected openPost(post: PostResponse) {
    this.selectedPost.set(post);
    this.loadFullPostMedia(post);
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
        this.revokePostMediaObjectUrl(post.postId);
        this.revokePostThumbnailObjectUrl(post.postId);
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

  protected postThumbnailUrl(post: PostResponse | null): string | null {
    if (!post) {
      return null;
    }

    return (
      this.postThumbnailObjectUrls()[post.postId]
      ?? this.directMediaUrl(post.mediaThumbnailUrl ?? post.mediaUrl)
    );
  }

  protected selectedPostMediaUrl(): string | null {
    const post = this.selectedPost();
    if (!post) {
      return null;
    }

    return this.postMediaObjectUrls()[post.postId] ?? this.directMediaUrl(post.mediaUrl);
  }

  protected selectedPostMediaKind(): PreviewKind {
    const post = this.selectedPost();
    if (!post) {
      return null;
    }

    return postMediaKind(post.mediaRef ?? post.mediaUrl);
  }

  protected postMediaKind(post: PostResponse): PreviewKind {
    return postMediaKind(post.mediaRef ?? post.mediaUrl);
  }

  private loadMyPosts() {
    this.loadingPosts.set(true);
    this.postService.getMyPosts().subscribe({
      next: posts => {
        this.myPosts.set(posts);
        this.loadPostMedia(posts);
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

  private loadProfileForRoute(username: string | null): void {
    this.publicProfileUsername.set(username);
    this.publicProfile.set(null);
    this.selectedPost.set(null);
    this.postPendingDeletion.set(null);
    this.myPosts.set([]);
    this.revokeProfilePictureObjectUrl();
    this.revokePostMediaObjectUrls();
    this.revokePostThumbnailObjectUrls();

    if (this.publicProfileMode()) {
      this.loadPublicProfile();
      return;
    }

    if (!this.createOnlyMode()) {
      this.loadProfilePicture(this.userService.user()?.profilePicture);
      this.loadMyPosts();
    }
  }

  private loadPublicProfile() {
    const username = this.publicProfileUsername();
    if (!username) {
      return;
    }

    this.loadingPosts.set(true);
    this.userService.getPublicProfile(username).subscribe({
      next: profile => {
        this.publicProfile.set(profile);
        this.loadProfilePicture(profile.profilePicture);
      },
      error: () => {
        this.loadingPosts.set(false);
        this.notificationService.showWarning(
          'This profile could not be loaded from the backend.',
          'Profile unavailable',
        );
      },
    });

    this.postService.getProfilePostsForUser(username).subscribe({
      next: posts => {
        this.myPosts.set(posts);
        this.loadPostMedia(posts);
        this.loadingPosts.set(false);
      },
      error: () => {
        this.myPosts.set([]);
        this.loadingPosts.set(false);
        this.notificationService.showWarning(
          'Posts for this profile could not be loaded from the backend.',
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

  private saveUsernameIfNeeded() {
    if (!this.usernameChanged()) {
      this.saveProfilePictureIfNeeded();
      return;
    }

    this.userService.updateUsername(
      { newUsername: this.usernameDraftTrimmed() },
      false,
    ).subscribe({
      next: () => this.saveProfilePictureIfNeeded(),
      error: () => this.updatingProfile.set(false),
    });
  }

  private saveProfilePictureIfNeeded() {
    const profilePicture = this.selectedProfilePicture();

    if (profilePicture) {
      this.userService.updateProfilePicture(profilePicture, false).subscribe({
        next: () => this.finishProfileSave(),
        error: () => this.updatingProfile.set(false),
      });
      return;
    }

    if (this.removeProfilePicture()) {
      this.userService.deleteProfilePicture(false).subscribe({
        next: () => this.finishProfileSave(),
        error: () => this.updatingProfile.set(false),
      });
      return;
    }

    this.finishProfileSave();
  }

  private finishProfileSave() {
    this.updatingProfile.set(false);
    this.editingProfile.set(false);
    this.selectedProfilePicture.set(null);
    this.removeProfilePicture.set(false);
    this.revokeAvatarPreview();
    this.loadProfilePicture(this.userService.user()?.profilePicture);
    this.notificationService.showSuccess('Updated profile');
  }

  private loadPostMedia(posts: PostResponse[]): void {
    this.revokePostThumbnailObjectUrls();
    posts.forEach(post => {
      const mediaUrl = post.mediaThumbnailUrl ?? post.mediaUrl;

      if (!mediaUrl) {
        return;
      }

      this.mediaCacheService.resolveMediaUrl(mediaUrl, 'image', 'normal').subscribe({
        next: objectUrl => {
          if (objectUrl) {
            this.setPostThumbnailObjectUrl(post.postId, objectUrl);
          }
        },
        error: () => this.loadPostThumbnailFallback(post),
      });
    });
  }

  private loadPostThumbnailFallback(post: PostResponse): void {
    if (!post.mediaThumbnailUrl || !post.mediaUrl || post.mediaThumbnailUrl === post.mediaUrl) {
      return;
    }

    this.mediaCacheService.resolveMediaUrl(
      post.mediaUrl,
      postMediaKind(post.mediaRef ?? post.mediaUrl),
      'normal',
    ).subscribe({
      next: objectUrl => {
        if (objectUrl) {
          this.setPostThumbnailObjectUrl(post.postId, objectUrl);
        }
      },
    });
  }

  private loadFullPostMedia(post: PostResponse): void {
    const mediaUrl = post.mediaUrl;

    if (!mediaUrl) {
      return;
    }

    if (this.postMediaObjectUrls()[post.postId]) {
      this.selectedPost.set(this.withRenderablePostMedia(post));
      return;
    }

    this.mediaCacheService.resolveMediaUrl(
      mediaUrl,
      postMediaKind(post.mediaRef ?? mediaUrl),
      'high',
    ).subscribe({
      next: objectUrl => {
        if (objectUrl) {
          this.setPostMediaObjectUrl(post.postId, objectUrl);
          this.selectedPost.set(this.withRenderablePostMedia(post));
        }
      },
    });
  }

  private loadProfilePicture(url: string | null | undefined): void {
    this.revokeProfilePictureObjectUrl();

    if (!url) {
      return;
    }

    this.mediaCacheService.resolveMediaUrl(url, 'image', 'high').subscribe({
      next: objectUrl => {
        if (objectUrl) {
          this.replaceProfilePictureObjectUrl(objectUrl);
        }
      },
      error: () => this.profilePictureObjectUrl.set(null),
    });
  }

  private isProtectedMediaUrl(url: string | null | undefined): boolean {
    return this.mediaCacheService.isBackendManagedMediaUrl(url);
  }

  private directMediaUrl(url: string | null | undefined): string | null {
    if (!url || this.isProtectedMediaUrl(url)) {
      return null;
    }

    return url;
  }

  private withRenderablePostMedia(post: PostResponse): PostResponse {
    const mediaUrl =
      this.postMediaObjectUrls()[post.postId] ?? this.directMediaUrl(post.mediaUrl);
    return mediaUrl ? { ...post, mediaUrl } : post;
  }

  private setPostMediaObjectUrl(postId: string, objectUrl: string): void {
    this.revokePostMediaObjectUrl(postId);
    this.postMediaObjectUrls.update(urls => ({ ...urls, [postId]: objectUrl }));
    this.selectedPost.update(post =>
      post?.postId === postId ? { ...post, mediaUrl: objectUrl } : post,
    );
  }

  private revokePostMediaObjectUrl(postId: string): void {
    this.postMediaObjectUrls.update(urls => {
      const remaining = { ...urls };
      delete remaining[postId];
      return remaining;
    });
  }

  private revokePostMediaObjectUrls(): void {
    this.postMediaObjectUrls.set({});
  }

  private setPostThumbnailObjectUrl(postId: string, objectUrl: string): void {
    this.revokePostThumbnailObjectUrl(postId);
    this.postThumbnailObjectUrls.update(urls => ({ ...urls, [postId]: objectUrl }));
  }

  private revokePostThumbnailObjectUrl(postId: string): void {
    this.postThumbnailObjectUrls.update(urls => {
      const remaining = { ...urls };
      delete remaining[postId];
      return remaining;
    });
  }

  private revokePostThumbnailObjectUrls(): void {
    this.postThumbnailObjectUrls.set({});
  }

  private replaceProfilePictureObjectUrl(objectUrl: string): void {
    this.profilePictureObjectUrl.set(objectUrl);
  }

  private revokeProfilePictureObjectUrl(): void {
    this.profilePictureObjectUrl.set(null);
  }

  private revokeAvatarPreview() {
    const currentAvatarPreviewUrl = this.avatarPreviewUrl();
    if (currentAvatarPreviewUrl) {
      URL.revokeObjectURL(currentAvatarPreviewUrl);
      this.avatarPreviewUrl.set(null);
    }
  }
}

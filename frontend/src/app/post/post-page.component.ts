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
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { TooltipModule } from 'primeng/tooltip';
import {Observable, Subscription, catchError, of, switchMap, forkJoin} from 'rxjs';

import { MediaCacheService } from '../core/services/media-cache.service';
import { usernameInitial } from '../core/utils/username-initial.utils';
import { NotificationService } from '../core/services/notification.service';
import { PublicUserProfile } from '../user/dto/PublicUserProfile';
import { UserService } from '../user/user.service';
import { ReportComponent } from '../report/components/report/report.component';
import { ContentTypeEnum } from '../report/dto/report';
import { USERNAME_PATTERN, USERNAME_VALIDATION_MESSAGE } from '../user/validators';
import { PostResponse } from './dto/PostResponse';
import { PostCaptionFormatPipe } from './post-caption-format.pipe';
import { TopicResponse } from './dto/TopicResponse';
import { PostComposerComponent } from './post-composer.component';
import { PostDetailDialogComponent } from './post-detail-dialog.component';
import { postMediaKind } from './post-media.utils';
import { PreviewKind, TopicOption } from './post-page.models';
import { PostService } from './post.service';
import {FollowBadgeComponent} from '../feed/components/follow-badge/follow-badge.component';
import {RelationshipService} from '../user/relationship.service';
import {FriendRequestEventBusService} from '../user/friend-request-event-bus.service';

@Component({
  selector: 'app-post-page',
  imports: [
    CommonModule,
    RouterModule,
    Button,
    Dialog,
    TooltipModule,
    PostCaptionFormatPipe,
    PostComposerComponent,
    PostDetailDialogComponent,
    ReportComponent,
    FollowBadgeComponent,
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
  private relationshipService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);
  private readonly avatarInput =
    viewChild<ElementRef<HTMLInputElement>>('avatarInput');

  protected readonly USERNAME_VALIDATION_MESSAGE = USERNAME_VALIDATION_MESSAGE;
  protected readonly dialogVisible = signal(false);
  protected readonly reportDialogVisible = signal(false);
  protected readonly avatarPreviewUrl = signal<string | null>(null);
  protected readonly profilePictureObjectUrl = signal<string | null>(null);
  protected readonly postMediaObjectUrls = signal<Record<string, string>>({});
  protected readonly postCardMediaUrls = signal<Record<string, string>>({});
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

  private readonly rel = inject(RelationshipService);

  protected readonly username = computed(
    () =>
      this.publicProfile()?.username
      ?? this.userService.user()?.username
      ?? 'Unknown user',
  );
  protected readonly avatarInitial = computed(() => usernameInitial(this.username()));
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
  protected readonly ContentTypeEnum = ContentTypeEnum;

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
    this.revokePostCardMediaUrls();
  }

  protected openCreateDialog() {
    this.dialogVisible.set(true);
  }

  protected openReportDialog() {
    this.reportDialogVisible.set(true);
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
      this.notificationService.showWarning(USERNAME_VALIDATION_MESSAGE);
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
        this.revokePostCardMediaUrl(post.postId);
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

  protected postCardMediaUrl(post: PostResponse | null): string | null {
    if (!post) {
      return null;
    }

    return this.postCardMediaUrls()[post.postId] ?? null;
  }

  protected postCardVideoUrl(post: PostResponse): string | null {
    const url = this.postCardMediaUrl(post);
    if (!url) {
      return null;
    }

    return this.withVideoStartFragment(url);
  }

  protected selectedPostMediaUrl(): string | null {
    const post = this.selectedPost();
    if (!post) {
      return null;
    }

    return this.postMediaObjectUrls()[post.postId] ?? this.directMediaUrl(post.mediaRef);
  }

  protected selectedPostMediaKind(): PreviewKind {
    const post = this.selectedPost();
    if (!post) {
      return null;
    }

    return postMediaKind(post.mediaRef);
  }

  protected postMediaKind(post: PostResponse): PreviewKind {
    return postMediaKind(post.mediaRef);
  }

  protected onPostCardMediaError(post: PostResponse): void {
    this.revokePostCardMediaUrl(post.postId);
  }

  protected primePostCardVideo(event: Event): void {
    const video = event.target as HTMLVideoElement | null;
    if (!video || !Number.isFinite(video.duration) || video.duration <= 0) {
      return;
    }

    try {
      if (video.currentTime < 0.05) {
        video.currentTime = Math.min(0.1, video.duration);
      }
      video.pause();
    } catch {
      // Some browsers do not allow programmatic seeking before enough data is available.
    }
  }

  private loadMyPosts() {
    this.loadingPosts.set(true);
    this.postService.getMyPosts().subscribe({
      next: posts => {
        console.log(posts)
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
    this.revokePostCardMediaUrls();

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
    if (!username) return;

    this.loadingPosts.set(true);

    forkJoin([
      this.userService.getPublicProfile(username),
      this.rel.loadFriends(),
      this.rel.loadFollowed(),
      this.rel.loadSentFriendRequests(),
      this.rel.loadIncomingFriendRequests(),
    ]).subscribe({
      next: ([profile]) => {
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
        console.log(posts)
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
    this.revokePostCardMediaUrls();
    posts.forEach(post => this.loadPostCardMedia(post));
  }

  private loadPostCardMedia(post: PostResponse): void {
    const mediaRef = post.mediaRef;
    if (!mediaRef) {
      return;
    }

    this.resolveVerifiedMediaUrl(mediaRef, postMediaKind(mediaRef)).subscribe(url => {
      if (url) {
        this.setPostCardMediaUrl(post.postId, url);
      }
    });
  }

  private resolveVerifiedMediaUrl(
    mediaRef: string,
    kind: PreviewKind,
  ): Observable<string | null> {
    return this.mediaCacheService.resolveMediaUrl(mediaRef, kind ?? 'image', 'normal').pipe(
      switchMap(url => {
        if (!url) {
          return of(null);
        }

        if (kind === 'video') {
          return of(url);
        }

        return this.verifyImageLoads(url);
      }),
      catchError(() => of(null)),
    );
  }

  private verifyImageLoads(url: string): Observable<string | null> {
    return new Observable(observer => {
      const image = new Image();
      image.decoding = 'async';
      image.onload = () => {
        observer.next(url);
        observer.complete();
      };
      image.onerror = () => {
        observer.next(null);
        observer.complete();
      };
      image.src = url;
    });
  }

  private loadFullPostMedia(post: PostResponse): void {
    if (!post.mediaRef) {
      return;
    }

    if (this.postMediaObjectUrls()[post.postId]) {
      return;
    }

    this.mediaCacheService.resolveMediaUrl(
      post.mediaRef,
      postMediaKind(post.mediaRef),
      'high',
    ).subscribe({
      next: objectUrl => {
        if (objectUrl) {
          this.setPostMediaObjectUrl(post.postId, objectUrl);
        }
      },
    });
  }

  private loadProfilePicture(url: string | null | undefined): void {
    this.revokeProfilePictureObjectUrl();

    const directUrl = this.directMediaUrl(url);
    if (directUrl) {
      this.replaceProfilePictureObjectUrl(directUrl);
      return;
    }

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

  private withVideoStartFragment(url: string): string {
    const fragmentIndex = url.indexOf('#');
    const baseUrl = fragmentIndex >= 0 ? url.slice(0, fragmentIndex) : url;
    return `${baseUrl}#t=0.1`;
  }

  private setPostMediaObjectUrl(postId: string, objectUrl: string): void {
    this.revokePostMediaObjectUrl(postId);
    this.postMediaObjectUrls.update(urls => ({ ...urls, [postId]: objectUrl }));
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

  private setPostCardMediaUrl(postId: string, objectUrl: string): void {
    this.revokePostCardMediaUrl(postId);
    this.postCardMediaUrls.update(urls => ({ ...urls, [postId]: objectUrl }));
  }

  private revokePostCardMediaUrl(postId: string): void {
    this.postCardMediaUrls.update(urls => {
      const remaining = { ...urls };
      delete remaining[postId];
      return remaining;
    });
  }

  private revokePostCardMediaUrls(): void {
    this.postCardMediaUrls.set({});
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

  blockUser() {
    if (this.username()) {
      this.relationshipService.block(this.username()).subscribe({
        next: () => {
          this.notificationService.showSuccess(`Blocked ${this.username()}`);
          },
        error: () => {
          this.notificationService.showError(`Failed to block ${this.username()}`);
        },
      });

    }
  }
}

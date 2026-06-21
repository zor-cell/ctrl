import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { TooltipModule } from 'primeng/tooltip';
import { Observable, Subscription, catchError, finalize, forkJoin, of, switchMap } from 'rxjs';

import { MediaCacheService } from '../core/services/media-cache.service';
import { previewMediaUrl } from '../core/utils/media-preview-url.utils';
import { usernameInitial } from '../core/utils/username-initial.utils';
import { NotificationService } from '../core/services/notification.service';
import { PublicUserProfile } from '../user/dto/PublicUserProfile';
import { UserService } from '../user/user.service';
import { ReportComponent } from '../report/components/report/report.component';
import { ContentTypeEnum } from '../report/dto/report';
import { USERNAME_PATTERN, USERNAME_VALIDATION_MESSAGE } from '../user/validators';
import { PostResponse } from './dto/PostResponse';
import { PostCaptionFormatPipe } from './post-caption-format.pipe';
import { TopicResponse } from '../topic/dto/TopicResponse';
import { PostComposerComponent } from './post-composer.component';
import { PostDetailDialogComponent } from './components/post-detail-dialog/post-detail-dialog.component';
import { PostShareDialogComponent } from './components/post-detail-share/post-share-dialog.component';
import { postMediaKind } from './post-media.utils';
import { PreviewKind, TopicOption } from './post-page.models';
import { PostService } from './post.service';
import { FollowBadgeComponent } from '../feed/components/follow-badge/follow-badge.component';
import { RelationshipService } from '../user/relationship.service';
import { AuthService } from '../auth/auth.service';
import { Tag } from 'primeng/tag';
import { FriendRequestEventBusService } from '../user/friend-request-event-bus.service';
import { Menu } from 'primeng/menu';
import { ChatService } from '../chat/services/chat.service';
import { Message } from 'primeng/message';
import { ConfirmationService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ShareablePost } from './dto/ShareablePost';

type ProfileFriendStatus = 'NONE' | 'FRIEND' | 'PENDING_REQUEST_SENT' | 'PENDING_REQUEST_INCOMING';

@Component({
  selector: 'app-post-page',
  imports: [
    CommonModule,
    RouterModule,
    Button,
    Dialog,
    Menu,
    TooltipModule,
    PostCaptionFormatPipe,
    PostComposerComponent,
    PostDetailDialogComponent,
    PostShareDialogComponent,
    ReportComponent,
    FollowBadgeComponent,
    Tag,
    Message,
    ConfirmDialog,
  ],
  providers: [ConfirmationService],
  templateUrl: './post-page.component.html',
  styleUrl: './post-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostPageComponent implements OnDestroy, OnInit {
  private readonly router = inject(Router);
  private confirmationService = inject(ConfirmationService);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly postService = inject(PostService);
  private readonly mediaCacheService = inject(MediaCacheService);
  private readonly notificationService = inject(NotificationService);
  private readonly chatService = inject(ChatService);
  protected readonly userService = inject(UserService);
  private readonly avatarInput = viewChild<ElementRef<HTMLInputElement>>('avatarInput');
  private relationshipService = inject(RelationshipService);
  private eventBus = inject(FriendRequestEventBusService);

  protected readonly isAdmin = computed(() => this.authService.isAdmin());
  protected readonly USERNAME_VALIDATION_MESSAGE = USERNAME_VALIDATION_MESSAGE;
  protected readonly dialogVisible = signal(false);
  protected readonly reportDialogVisible = signal(false);
  protected readonly avatarPreviewUrl = signal<string | null>(null);
  protected readonly profilePictureObjectUrl = signal<string | null>(null);
  protected readonly postMediaObjectUrls = signal<Record<string, string>>({});
  protected readonly postCardMediaUrls = signal<Record<string, string>>({});
  protected readonly myPosts = signal<PostResponse[]>([]);
  protected readonly selectedPostId = signal<string | null>(null);
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
  protected readonly blockDialogVisible = signal(false);
  protected readonly blockingUser = signal(false);
  protected readonly loadingTopics = signal(false);
  protected readonly topicOptions = signal<TopicOption[]>([]);
  protected readonly friendActionBusy = signal(false);
  protected readonly shareDialogVisible = signal(false);
  protected readonly selectedPostForShare = signal<PostResponse | null>(null);
  @ViewChild(PostShareDialogComponent) private readonly shareDialog?: PostShareDialogComponent;

  private readonly rel = inject(RelationshipService);
  protected readonly friends = this.rel.friends;
  protected readonly pendingCount = computed(() => this.relationshipService.pendingRequestCount());
  private readonly profileFriendStatusOverride = signal<{
    username: string;
    status: ProfileFriendStatus;
  } | null>(null);

  protected readonly username = computed(
    () =>
      this.publicProfile()?.username ??
      this.publicProfileUsername() ??
      this.userService.user()?.username ??
      'Unknown user',
  );
  protected readonly profileTargetUsername = computed(
    () => this.publicProfile()?.username ?? this.publicProfileUsername(),
  );
  protected readonly profileActionUsername = computed(() => this.publicProfile()?.username ?? null);
  protected readonly avatarInitial = computed(() => usernameInitial(this.username()));
  protected readonly currentProfilePicture = computed(() =>
    this.removeProfilePicture()
      ? null
      : (this.avatarPreviewUrl() ?? this.profilePictureObjectUrl() ?? null),
  );
  protected readonly userBio = computed(() => {
    const bio = this.publicProfileMode()
      ? this.publicProfile()?.bio?.trim()
      : this.userService.user()?.bio?.trim();
    return bio && bio.length > 0 ? bio : 'No bio yet.';
  });
  protected readonly publicProfileMode = computed(() => this.publicProfileUsername() !== null);
  protected readonly isOwnPublicProfile = computed(() => {
    const profile = this.publicProfile();
    const currentUser = this.userService.user();
    if (profile && currentUser) {
      return profile.id.toLowerCase() === currentUser.id.toLowerCase();
    }

    const profileUsername = this.profileTargetUsername();
    const currentUsername = this.userService.user()?.username;

    return Boolean(
      profileUsername &&
      currentUsername &&
      profileUsername.toLowerCase() === currentUsername.toLowerCase(),
    );
  });
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
    () =>
      !this.usernameEmpty() &&
      (this.usernameDraftTrimmed().length < 3 ||
        !USERNAME_PATTERN.test(this.usernameDraftTrimmed())),
  );
  protected readonly canSaveProfile = computed(
    () =>
      !this.updatingProfile() &&
      !this.bioTooLong() &&
      !this.usernameEmpty() &&
      !this.usernameInvalid() &&
      !this.checkingUsername() &&
      this.usernameAvailable() !== false,
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
  protected readonly publicProfilePrivateAccess = computed(
    () =>
      !this.publicProfileMode() ||
      this.isOwnPublicProfile() ||
      this.publicProfileFriendStatus() === 'FRIEND',
  );
  protected readonly privatePostsHidden = computed(
    () => this.publicProfileMode() && !this.publicProfilePrivateAccess(),
  );
  protected readonly publicProfileFriendStatus = computed<ProfileFriendStatus>(() => {
    const username = this.profileActionUsername()?.toLowerCase();
    const profile = this.publicProfile();

    if (!username || this.isOwnPublicProfile()) {
      return 'NONE';
    }

    const override = this.profileFriendStatusOverride();
    if (override?.username.toLowerCase() === username) {
      return override.status;
    }

    if (this.isFriendWithProfile(username, profile?.id) || profile?.canSeePrivatePosts === true) {
      return 'FRIEND';
    }

    if (
      this.rel.incomingRequests().some((request) => request.username.toLowerCase() === username)
    ) {
      return 'PENDING_REQUEST_INCOMING';
    }

    if (this.rel.sentRequests().some((request) => request.username.toLowerCase() === username)) {
      return 'PENDING_REQUEST_SENT';
    }

    return 'NONE';
  });
  protected readonly selectedShareablePost = computed<ShareablePost | null>(() => {
    const post = this.selectedPostForShare();
    if (!post) {
      return null;
    }

    return {
      postId: post.postId,
      username: post.username,
      caption: post.caption,
      mediaUrl:
        this.postMediaObjectUrls()[post.postId] ??
        this.postCardMediaUrls()[post.postId] ??
        this.directMediaUrl(post.mediaRef),
    };
  });
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
  protected readonly isVerifiedUser = computed(() => this.userService.user()?.isVerified ?? false);
  protected readonly maxBioLength = 280;
  protected readonly createOnlyMode = signal(false);
  protected readonly ContentTypeEnum = ContentTypeEnum;
  protected readonly publicProfileMenuItems = computed(() => {
    const items = [
      {
        label: 'Report',
        icon: 'pi pi-flag',
        command: () => this.openReportDialog(),
      },
      {
        label: 'Block',
        icon: 'pi pi-ban',
        command: () => this.openBlockConfirmation(),
      },
    ];

    if (this.isAdmin()) {
      items.push({
        label: 'Remove',
        icon: 'pi pi-trash',
        command: () => this.deleteAccount(),
      });
    }

    return items;
  });

  private usernameCheckTimeout: ReturnType<typeof setTimeout> | null = null;
  private routeParamsSubscription: Subscription | null = null;

  ngOnInit() {
    this.createOnlyMode.set(this.route.snapshot.routeConfig?.path === 'post');
    this.loadTopics();
    this.relationshipService.loadPendingRequestCount().subscribe();

    this.routeParamsSubscription = this.route.paramMap.subscribe((params) => {
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

  protected sendProfileFriendRequest(): void {
    const username = this.profileActionUsername();

    if (
      !username ||
      this.isOwnPublicProfile() ||
      this.publicProfileFriendStatus() !== 'NONE' ||
      this.friendActionBusy()
    ) {
      return;
    }

    this.friendActionBusy.set(true);
    this.rel
      .sendFriendRequest(username)
      .pipe(finalize(() => this.friendActionBusy.set(false)))
      .subscribe({
        next: () => {
          this.setProfileFriendStatusOverride(username, 'PENDING_REQUEST_SENT');
          this.notificationService.showSuccess(`Friend request sent to ${username}`);
        },
        error: () => {
          this.notificationService.showError('Failed to send friend request');
        },
      });
  }

  protected acceptProfileFriendRequest(): void {
    const username = this.profileActionUsername();

    if (
      !username ||
      this.isOwnPublicProfile() ||
      this.publicProfileFriendStatus() !== 'PENDING_REQUEST_INCOMING' ||
      this.friendActionBusy()
    ) {
      return;
    }

    this.friendActionBusy.set(true);
    this.rel
      .acceptFriendRequest(username)
      .pipe(finalize(() => this.friendActionBusy.set(false)))
      .subscribe({
        next: () => {
          this.setProfileFriendStatusOverride(username, 'FRIEND');
          this.notificationService.showSuccess(`Accepted friend request from ${username}`);
        },
        error: () => {
          this.notificationService.showError('Failed to accept friend request');
        },
      });
  }

  protected cancelProfileFriendRequest(): void {
    const username = this.profileActionUsername();

    if (
      !username ||
      this.isOwnPublicProfile() ||
      this.publicProfileFriendStatus() !== 'PENDING_REQUEST_SENT' ||
      this.friendActionBusy()
    ) {
      return;
    }

    this.friendActionBusy.set(true);
    this.rel
      .cancelFriendRequest(username)
      .pipe(finalize(() => this.friendActionBusy.set(false)))
      .subscribe({
        next: () => {
          this.setProfileFriendStatusOverride(username, 'NONE');
          this.notificationService.showSuccess(`Friend request to ${username} cancelled`);
        },
        error: () => {
          this.notificationService.showError('Failed to cancel friend request');
        },
      });
  }

  protected removeProfileFriend(): void {
    const username = this.profileActionUsername();

    if (
      !username ||
      this.isOwnPublicProfile() ||
      this.publicProfileFriendStatus() !== 'FRIEND' ||
      this.friendActionBusy()
    ) {
      return;
    }

    this.friendActionBusy.set(true);
    this.rel
      .unfriend(username)
      .pipe(finalize(() => this.friendActionBusy.set(false)))
      .subscribe({
        next: () => {
          this.setProfileFriendStatusOverride(username, 'NONE');
          this.removePublicProfilePrivateAccess();
          this.rel.loadFriends().subscribe();
          this.notificationService.showSuccess(`Removed ${username} from friends`);
        },
        error: () => {
          this.notificationService.showError('Failed to remove friend');
        },
      });
  }

  protected openDirectMessage(): void {
    const username = this.profileActionUsername();
    if (!username || this.publicProfileFriendStatus() !== 'FRIEND') {
      return;
    }

    const currentUserId = this.userService.user()?.id?.toLowerCase();

    this.chatService.getChats().subscribe({
      next: (chats) => {
        const directChat = chats.find((chat) => {
          if (chat.groupChat) return false;
          return chat.participants.some(
            (p) => p.username.toLowerCase() === username.toLowerCase() &&
              (currentUserId ? p.id.toLowerCase() !== currentUserId : true),
          );
        });

        const queryParams = directChat
          ? { chat: directChat.chatId }
          : { user: username };

        this.router.navigate(['/chat'], { queryParams });
      },
      error: () => {
        this.router.navigate(['/chat'], { queryParams: { user: username } });
      },
    });
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
    this.router.navigate(['/settings'], { queryParams: { tab: 'account' } });
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
        next: (available) => {
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
    this.userService
      .updateUser(
        {
          name: user.name,
          email: user.email,
          bio: this.profileBioDraft().trim(),
          birthdate: user.birthdate,
        },
        false,
      )
      .subscribe({
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
      this.notificationService.showWarning('Please choose an image as profile picture.');
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
    return this.topicOptions().find((option) => option.value === topicId)?.label ?? topicId;
  }

  protected openPost(post: PostResponse) {
    this.selectedPostId.set(post.postId);
  }

  protected closePost() {
    this.selectedPostId.set(null);
  }

  protected onPostDeletedFromDialog(postId: string): void {
    this.revokePostMediaObjectUrl(postId);
    this.revokePostCardMediaUrl(postId);
    this.myPosts.update((posts) => posts.filter((p) => p.postId !== postId));
    this.selectedPostId.set(null);
  }

  protected requestPostDeletion(post: PostResponse) {
    this.postPendingDeletion.set(post);
  }

  protected openShareDialog(post: PostResponse): void {
    this.selectedPostForShare.set(post);
    this.selectedPostId.set(null);
    this.shareDialogVisible.set(true);
    this.shareDialog?.open();
  }

  protected onShareDialogVisibleChange(visible: boolean): void {
    this.shareDialogVisible.set(visible);
    if (!visible) {
      this.selectedPostForShare.set(null);
    }
  }

  protected cancelPostDeletion() {
    if (this.deletingPost()) {
      return;
    }

    this.postPendingDeletion.set(null);
  }

  protected openBlockConfirmation(): void {
    if (!this.profileActionUsername() || this.blockingUser()) {
      return;
    }

    this.blockDialogVisible.set(true);
  }

  protected cancelBlockConfirmation(): void {
    if (this.blockingUser()) {
      return;
    }

    this.blockDialogVisible.set(false);
  }

  protected confirmBlockUser(): void {
    const username = this.profileActionUsername();

    if (!username || this.blockingUser()) {
      return;
    }

    this.blockingUser.set(true);
    this.relationshipService.block(username).subscribe({
      next: () => {
        this.blockDialogVisible.set(false);
        this.blockingUser.set(false);
        this.notificationService.showSuccess(`Blocked ${username}`);
        this.router.navigate(['/followed'], { queryParams: { tab: 'blocked' } });
      },
      error: () => {
        this.blockingUser.set(false);
        this.notificationService.showError(`Failed to block ${username}`);
      },
    });
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
        this.myPosts.update((posts) =>
          posts.filter((currentPost) => currentPost.postId !== post.postId),
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
      next: (posts) => {
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
    const isOwnProfileRoute = this.isCurrentUserProfileUsername(username);

    this.publicProfileUsername.set(isOwnProfileRoute ? null : username);
    this.publicProfile.set(null);
    this.selectedPostId.set(null);
    this.postPendingDeletion.set(null);
    this.friendActionBusy.set(false);
    this.profileFriendStatusOverride.set(null);
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
        if (this.isOwnProfile(profile)) {
          this.router.navigate(['/profile'], { replaceUrl: true });
          this.publicProfileUsername.set(null);
          this.publicProfile.set(null);
          this.loadProfilePicture(this.userService.user()?.profilePicture);
          this.loadMyPosts();
          return;
        }

        this.publicProfile.set(profile);
        this.loadProfilePicture(profile.profilePicture);
        this.loadPublicProfilePosts(username);
      },
      error: () => {
        this.myPosts.set([]);
        this.loadingPosts.set(false);
        this.notificationService.showWarning(
          'This profile could not be loaded from the backend.',
          'Profile unavailable',
        );
      },
    });
  }

  private loadPublicProfilePosts(username: string): void {
    this.postService.getProfilePostsForUser(username).subscribe({
      next: (posts) => {
        const visiblePosts = this.visiblePublicProfilePosts(posts);
        this.myPosts.set(visiblePosts);
        this.loadPostMedia(visiblePosts);
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
      next: (topics) => {
        this.topicOptions.set(topics.map((topic) => this.toTopicOption(topic)));
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

  private setProfileFriendStatusOverride(username: string, status: ProfileFriendStatus): void {
    this.profileFriendStatusOverride.set({ username, status });
  }

  private visiblePublicProfilePosts(posts: PostResponse[]): PostResponse[] {
    if (!this.publicProfileMode() || this.publicProfilePrivateAccess()) {
      return posts;
    }

    return posts.filter((post) => post.isPublic);
  }

  private removePublicProfilePrivateAccess(): void {
    const profile = this.publicProfile();
    if (profile) {
      this.publicProfile.set({ ...profile, canSeePrivatePosts: false });
    }

    const visiblePosts = this.visiblePublicProfilePosts(this.myPosts());
    if (visiblePosts.length !== this.myPosts().length) {
      // Close dialog if the currently open post is no longer visible
      const openId = this.selectedPostId();
      if (openId && !visiblePosts.some((p) => p.postId === openId)) {
        this.selectedPostId.set(null);
      }
      this.myPosts.set(visiblePosts);
      this.loadPostMedia(visiblePosts);
    }
  }

  private isOwnProfile(profile: PublicUserProfile): boolean {
    const currentUserId = this.userService.user()?.id;
    return Boolean(currentUserId && profile.id.toLowerCase() === currentUserId.toLowerCase());
  }

  private isFriendWithProfile(username: string, profileId: string | undefined): boolean {
    const normalizedProfileId = profileId?.toLowerCase();

    return this.rel
      .friends()
      .some(
        (friend) =>
          friend.username.toLowerCase() === username ||
          Boolean(normalizedProfileId && friend.id.toLowerCase() === normalizedProfileId),
      );
  }

  private isCurrentUserProfileUsername(username: string | null): boolean {
    const currentUsername = this.userService.user()?.username;

    return Boolean(
      username && currentUsername && username.toLowerCase() === currentUsername.toLowerCase(),
    );
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

    this.userService.updateUsername({ newUsername: this.usernameDraftTrimmed() }, false).subscribe({
      next: () => this.saveProfilePictureIfNeeded(),
      error: () => this.updatingProfile.set(false),
    });
  }

  protected deleteAccount() {
    if (!this.username()) {
      return;
    }

    this.confirmationService.confirm({
      message: 'Are you sure you want remove this account?',
      header: 'Confirm Remove',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.authService.requestAccountDeletion(this.username()).subscribe({
          next: () => this.loadPublicProfile(),
          error: () =>
            this.notificationService.showError('Failed to delete account. Please try again later.'),
        });
      },
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
    posts.forEach((post) => this.loadPostCardMedia(post));
  }

  private loadPostCardMedia(post: PostResponse): void {
    const mediaRef = post.mediaRef;
    if (!mediaRef) {
      return;
    }

    this.resolveVerifiedMediaUrl(mediaRef, postMediaKind(mediaRef)).subscribe((url) => {
      if (url) {
        this.setPostCardMediaUrl(post.postId, previewMediaUrl(url) ?? url);
      }
    });
  }

  private resolveVerifiedMediaUrl(
    mediaRef: string,
    kind: PreviewKind,
    priority: 'normal' | 'high' = 'normal',
  ): Observable<string | null> {
    return this.mediaCacheService.resolveMediaUrl(mediaRef, kind ?? 'image', priority).pipe(
      switchMap((url) => {
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
    return new Observable((observer) => {
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

    this.resolveVerifiedMediaUrl(post.mediaRef, postMediaKind(post.mediaRef), 'high').subscribe({
      next: (objectUrl) => {
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
      next: (objectUrl) => {
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
    this.postMediaObjectUrls.update((urls) => ({ ...urls, [postId]: objectUrl }));
  }

  private revokePostMediaObjectUrl(postId: string): void {
    this.postMediaObjectUrls.update((urls) => {
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
    this.postCardMediaUrls.update((urls) => ({ ...urls, [postId]: objectUrl }));
  }

  private revokePostCardMediaUrl(postId: string): void {
    this.postCardMediaUrls.update((urls) => {
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
}

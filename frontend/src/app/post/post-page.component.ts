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
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { TooltipModule } from 'primeng/tooltip';

import { NotificationService } from '../core/services/notification.service';
import { PublicUserProfile } from '../user/dto/PublicUserProfile';
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
  imports: [
    CommonModule,
    Button,
    Dialog,
    TooltipModule,
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
  private readonly notificationService = inject(NotificationService);
  protected readonly userService = inject(UserService);
  private readonly avatarInput =
    viewChild<ElementRef<HTMLInputElement>>('avatarInput');

  protected readonly dialogVisible = signal(false);
  protected readonly avatarPreviewUrl = signal<string | null>(null);
  protected readonly myPosts = signal<PostResponse[]>([]);
  protected readonly publicProfile = signal<PublicUserProfile | null>(null);
  protected readonly selectedPost = signal<PostResponse | null>(null);
  protected readonly loadingPosts = signal(false);
  protected readonly updatingAvatar = signal(false);
  protected readonly editingProfile = signal(false);
  protected readonly updatingProfile = signal(false);
  protected readonly profileBioDraft = signal('');
  protected readonly profileUsernameDraft = signal('');
  protected readonly selectedProfilePicture = signal<File | null>(null);
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
      this.avatarPreviewUrl()
      ?? this.publicProfile()?.profilePicture
      ?? this.userService.user()?.profilePicture
      ?? null,
  );
  protected readonly userBio = computed(() => {
    const bio = (this.publicProfile()?.bio ?? this.userService.user()?.bio)?.trim();
    return bio && bio.length > 0 ? bio : 'No bio yet.';
  });
  protected readonly publicProfileUsername = signal<string | null>(null);
  protected readonly publicProfileMode = computed(() => this.publicProfileUsername() !== null);
  protected readonly canEditProfile = computed(
    () => !this.createOnlyMode() && !this.publicProfileMode(),
  );
  protected readonly bioLength = computed(() => this.profileBioDraft().length);
  protected readonly bioTooLong = computed(() => this.bioLength() > this.maxBioLength);
  protected readonly usernameDraftTrimmed = computed(() => this.profileUsernameDraft().trim());
  protected readonly usernameChanged = computed(
    () => this.usernameDraftTrimmed() !== this.username(),
  );
  protected readonly usernameEmpty = computed(() => this.usernameDraftTrimmed().length === 0);
  protected readonly canSaveProfile = computed(
    () =>
      !this.updatingProfile()
      && !this.bioTooLong()
      && !this.usernameEmpty()
      && !this.checkingUsername()
      && (this.usernameAvailable() !== false),
  );
  protected readonly postsCountLabel = computed(() => {
    const count = this.myPosts().length;
    return count === 1 ? '1 post' : `${count} posts`;
  });
  protected readonly isVerifiedUser = computed(
    () => this.userService.user()?.isVerified ?? false,
  );
  protected readonly profileEyebrow = computed(() =>
    this.publicProfileMode() ? 'Profile' : 'Archive',
  );
  protected readonly postsHeading = computed(() =>
    this.publicProfileMode() ? `${this.username()}'s posts` : 'Your posts',
  );
  protected readonly maxBioLength = 280;
  protected readonly createOnlyMode = signal(false);
  private usernameAvailabilityTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit() {
    this.createOnlyMode.set(this.route.snapshot.routeConfig?.path === 'post');
    this.publicProfileUsername.set(this.route.snapshot.paramMap.get('username'));
    this.loadTopics();
    const profileUsername = this.publicProfileUsername();
    if (profileUsername) {
      this.loadPublicProfile(profileUsername);
      this.loadProfilePosts(profileUsername);
    } else if (!this.createOnlyMode()) {
      this.loadMyPosts();
    }

    if (this.createOnlyMode()) {
      this.openCreateDialog();
    }
  }

  ngOnDestroy() {
    if (this.usernameAvailabilityTimer) {
      clearTimeout(this.usernameAvailabilityTimer);
    }
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
    if (!this.canEditProfile()) {
      return;
    }

    this.router.navigate(['/settings']);
  }

  protected startProfileEdit() {
    if (!this.canEditProfile()) {
      return;
    }

    const user = this.userService.user();
    this.profileBioDraft.set(user?.bio ?? '');
    this.profileUsernameDraft.set(user?.username ?? '');
    this.usernameAvailable.set(true);
    this.checkingUsername.set(false);
    this.selectedProfilePicture.set(null);
    this.revokeAvatarPreview();
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
    this.checkingUsername.set(false);
    this.selectedProfilePicture.set(null);
    this.revokeAvatarPreview();
  }

  protected updateBioDraft(event: Event) {
    const input = event.target as HTMLTextAreaElement;
    this.profileBioDraft.set(input.value);
  }

  protected updateUsernameDraft(event: Event) {
    const input = event.target as HTMLInputElement;
    this.profileUsernameDraft.set(input.value);
    this.scheduleUsernameAvailabilityCheck();
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

    if (this.usernameEmpty()) {
      this.notificationService.showWarning('Username is required.');
      return;
    }

    if (this.usernameAvailable() === false) {
      this.notificationService.showWarning('This username is already taken.');
      return;
    }

    this.updatingProfile.set(true);
    this.userService
      .updateUser({
        name: user.name,
        email: user.email,
        bio: this.profileBioDraft().trim(),
        birthdate: user.birthdate,
      }, false)
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
      this.notificationService.showWarning(
        'Please choose an image as profile picture.',
      );
      input.value = '';
      return;
    }

    this.revokeAvatarPreview();
    this.avatarPreviewUrl.set(URL.createObjectURL(file));
    this.selectedProfilePicture.set(file);
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

  private loadPublicProfile(username: string) {
    this.userService.getPublicProfile(username).subscribe({
      next: profile => {
        this.publicProfile.set(profile);
      },
      error: () => {
        this.notificationService.showWarning(
          'This profile could not be loaded from the backend.',
          'Profile unavailable',
        );
        this.router.navigate(['/search']);
      },
    });
  }

  private loadProfilePosts(username: string) {
    this.loadingPosts.set(true);
    this.postService.getProfilePostsForUser(username).subscribe({
      next: posts => {
        this.myPosts.set(posts);
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

  private revokeAvatarPreview() {
    const currentAvatarPreviewUrl = this.avatarPreviewUrl();
    if (currentAvatarPreviewUrl) {
      URL.revokeObjectURL(currentAvatarPreviewUrl);
      this.avatarPreviewUrl.set(null);
    }
  }

  private scheduleUsernameAvailabilityCheck() {
    if (this.usernameAvailabilityTimer) {
      clearTimeout(this.usernameAvailabilityTimer);
    }

    const username = this.usernameDraftTrimmed();
    if (!username) {
      this.usernameAvailable.set(false);
      this.checkingUsername.set(false);
      return;
    }

    if (!this.usernameChanged()) {
      this.usernameAvailable.set(true);
      this.checkingUsername.set(false);
      return;
    }

    this.checkingUsername.set(true);
    this.usernameAvailable.set(null);
    this.usernameAvailabilityTimer = setTimeout(() => {
      this.userService.isUsernameAvailable(username).subscribe({
        next: available => {
          if (this.usernameDraftTrimmed() === username) {
            this.usernameAvailable.set(available);
            this.checkingUsername.set(false);
          }
        },
        error: () => {
          this.usernameAvailable.set(null);
          this.checkingUsername.set(false);
        },
      });
    }, 350);
  }

  private saveUsernameIfNeeded() {
    if (!this.usernameChanged()) {
      this.saveProfilePictureIfNeeded();
      return;
    }

    this.userService.updateUsername({ newUsername: this.usernameDraftTrimmed() }, false).subscribe({
      next: () => this.saveProfilePictureIfNeeded(),
      error: error => {
        this.updatingProfile.set(false);
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.usernameAvailable.set(false);
        }
      },
    });
  }

  private saveProfilePictureIfNeeded() {
    const file = this.selectedProfilePicture();
    if (!file) {
      this.finishProfileUpdate();
      return;
    }

    this.updatingAvatar.set(true);
    this.userService.updateProfilePicture(file, false).subscribe({
      next: () => {
        this.updatingAvatar.set(false);
        this.finishProfileUpdate();
      },
      error: () => {
        this.updatingAvatar.set(false);
        this.updatingProfile.set(false);
      },
    });
  }

  private finishProfileUpdate() {
    this.updatingProfile.set(false);
    this.editingProfile.set(false);
    this.selectedProfilePicture.set(null);
    this.profileBioDraft.set('');
    this.profileUsernameDraft.set('');
    this.usernameAvailable.set(null);
    this.revokeAvatarPreview();
    this.notificationService.showSuccess('Updated profile');
  }
}

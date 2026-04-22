import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { MultiSelect } from 'primeng/multiselect';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { NotificationService } from '../core/services/notification.service';
import { UserService } from '../user/user.service';
import { CreatePostRequest } from './dto/CreatePostRequest';
import { CreateSingleSourceRequest } from './dto/CreateSingleSourceRequest';
import { CreateSourceRequest } from './dto/CreateSourceRequest';
import { PostResponse, SingleSourceResponse } from './dto/PostResponse';
import { TopicResponse } from './dto/TopicResponse';
import { FileService } from './file.service';
import { PostService } from './post.service';

interface TopicOption {
  label: string;
  value: string;
}

type PreviewKind = 'image' | 'video' | null;

const MAX_POST_MEDIA_SIZE_BYTES = 100 * 1024 * 1024;
const MAX_POST_MEDIA_SIZE_LABEL = '100 MB';
const ACCEPTED_POST_MEDIA_FORMATS_LABEL =
  'JPG, PNG, WEBP, GIF, MP4, WEBM, MOV';
const ACCEPTED_POST_MEDIA_INPUT_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
  'video/mp4',
  'video/webm',
  'video/quicktime',
  'video/x-quicktime',
  '.mov',
].join(',');
const ALLOWED_POST_MEDIA_TYPES = new Set([
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
  'video/mp4',
  'video/webm',
  'video/quicktime',
  'video/x-quicktime',
]);
const ALLOWED_POST_MEDIA_EXTENSIONS = new Set([
  'jpg',
  'jpeg',
  'png',
  'webp',
  'gif',
  'mp4',
  'webm',
  'mov',
]);

interface SourceFormValue {
  journal: string;
  author: string;
  title: string;
  url: string;
  date: string | Date | null;
}

@Component({
  selector: 'app-post-page',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Button,
    Dialog,
    DatePicker,
    FloatLabel,
    InputText,
    MultiSelect,
    Textarea,
    ToggleSwitch,
  ],
  templateUrl: './post-page.component.html',
  styleUrl: './post-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostPageComponent implements OnDestroy, OnInit {
  protected readonly maxCaptionLength = 10000;
  protected readonly maxSources = 10;
  protected readonly maxSourceJournalLength = 255;
  protected readonly maxSourceAuthorLength = 255;
  protected readonly maxSourceTitleLength = 500;
  protected readonly maxSourceUrlLength = 2048;
  protected readonly maxPostMediaSizeLabel = MAX_POST_MEDIA_SIZE_LABEL;
  protected readonly acceptedPostMediaFormatsLabel =
    ACCEPTED_POST_MEDIA_FORMATS_LABEL;
  protected readonly acceptedPostMediaInputTypes =
    ACCEPTED_POST_MEDIA_INPUT_TYPES;
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly postService = inject(PostService);
  private readonly fileService = inject(FileService);
  private readonly notificationService = inject(NotificationService);
  protected readonly userService = inject(UserService);
  private readonly mediaInput =
    viewChild<ElementRef<HTMLInputElement>>('mediaInput');
  private readonly avatarInput =
    viewChild<ElementRef<HTMLInputElement>>('avatarInput');

  protected readonly dialogVisible = signal(false);
  protected readonly currentStep = signal(1);
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly avatarPreviewUrl = signal<string | null>(null);
  protected readonly previewKind = signal<PreviewKind>(null);
  protected readonly selectedFileName = signal<string | null>(null);
  protected readonly selectedMediaFile = signal<File | null>(null);
  protected readonly myPosts = signal<PostResponse[]>([]);
  protected readonly selectedPost = signal<PostResponse | null>(null);
  protected readonly sourcesExpanded = signal(false);
  protected readonly loadingPosts = signal(false);
  protected readonly updatingAvatar = signal(false);
  protected readonly submitting = signal(false);
  protected readonly deletingPost = signal(false);
  protected readonly postPendingDeletion = signal<PostResponse | null>(null);
  protected readonly loadingTopics = signal(false);
  protected readonly submitAttempted = signal(false);
  protected readonly expandedSourceIndex = signal<number | null>(0);
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
  protected readonly selectedPostMediaUrl = computed(() =>
    this.postMediaUrl(this.selectedPost()),
  );
  protected readonly postsCountLabel = computed(() => {
    const count = this.myPosts().length;
    return count === 1 ? '1 post' : `${count} posts`;
  });
  protected readonly isVerifiedUser = computed(
    () => this.userService.user()?.isVerified ?? false,
  );
  protected readonly selectedTopicsSummary = computed(() => {
    const selectedValues = this.postForm.controls.topicIds.value;
    return this.topicOptions()
      .filter(option => selectedValues.includes(option.value))
      .map(option => option.label)
      .join(', ');
  });
  protected readonly postForm = this.fb.group({
    caption: this.fb.nonNullable.control('', [
      Validators.required,
      this.notBlankValidator(),
      Validators.maxLength(this.maxCaptionLength),
    ]),
    isPublic: this.fb.nonNullable.control(false),
    isAiGenerated: this.fb.nonNullable.control(false),
    topicIds: this.fb.nonNullable.control<string[]>([]),
    sources: this.fb.array([this.createSourceGroup()]),
  });

  ngOnInit() {
    this.loadTopics();
    this.loadMyPosts();
  }

  ngOnDestroy() {
    this.revokePreview();
    this.revokeAvatarPreview();
  }

  protected openCreateDialog() {
    this.dialogVisible.set(true);
    this.currentStep.set(1);
  }

  protected closeCreateDialog() {
    this.dialogVisible.set(false);
    this.currentStep.set(1);
    this.resetFormState();
  }

  protected openProfileSettings() {
    this.router.navigate(['/profile']);
  }

  protected openMediaPicker() {
    this.mediaInput()?.nativeElement.click();
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

  protected onMediaSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);

    if (!file) {
      return;
    }

    if (!this.isValidPostMedia(file)) {
      input.value = '';
      this.clearSelectedMedia();
      return;
    }

    this.revokePreview();
    this.selectedMediaFile.set(file);
    this.selectedFileName.set(file.name);
    this.previewUrl.set(URL.createObjectURL(file));
    this.previewKind.set(this.resolvePreviewKind(file));
  }

  protected continueToDetails() {
    this.currentStep.set(2);
  }

  protected goBackToMedia() {
    this.currentStep.set(1);
  }

  protected canAddSource(): boolean {
    return this.sources.length < this.maxSources;
  }

  protected addSource() {
    if (!this.canAddSource()) {
      this.notificationService.showWarning(
        `You can add at most ${this.maxSources} sources per post.`,
      );
      return;
    }

    this.sources.push(this.createSourceGroup());
    this.expandedSourceIndex.set(this.sources.length - 1);
  }

  protected removeSource(index: number) {
    if (this.sources.length === 1) {
      this.sources.at(0).reset({
        journal: '',
        author: '',
        title: '',
        url: '',
        date: '',
      });
      this.expandedSourceIndex.set(0);
      return;
    }

    this.sources.removeAt(index);

    const expandedIndex = this.expandedSourceIndex();
    if (expandedIndex === null) {
      return;
    }

    if (expandedIndex === index) {
      this.expandedSourceIndex.set(Math.min(index, this.sources.length - 1));
      return;
    }

    if (expandedIndex > index) {
      this.expandedSourceIndex.set(expandedIndex - 1);
    }
  }

  protected toggleSource(index: number) {
    this.expandedSourceIndex.set(this.isSourceExpanded(index) ? null : index);
  }

  protected isSourceExpanded(index: number): boolean {
    return this.expandedSourceIndex() === index;
  }

  protected collapsedSourceTitle(index: number): string {
    const source = this.sources.at(index).value as SourceFormValue;
    const title = source.title.trim();
    return title === '' ? 'Untitled source' : title;
  }

  protected hasCollapsedSourceError(index: number): boolean {
    return (
      this.submitAttempted()
      && !this.isSourceExpanded(index)
      && this.sources.at(index).invalid
    );
  }

  protected submitPost() {
    this.submitAttempted.set(true);

    if (this.postForm.invalid) {
      this.postForm.markAllAsTouched();
      return;
    }

    const media = this.selectedMediaFile();

    if (!media) {
      this.createPostWithMediaRef(null);
      return;
    }

    if (!this.isValidPostMedia(media)) {
      this.currentStep.set(1);
      this.clearSelectedMedia();
      return;
    }

    this.submitting.set(true);

    this.fileService.uploadPostMedia(media).subscribe({
      next: uploadedFile => this.createPostWithMediaRef(uploadedFile.mediaRef),
      error: () => {
        this.submitting.set(false);
      },
    });
  }

  protected sourceGroupAt(index: number): AbstractControl {
    return this.sources.at(index);
  }

  protected hasSourceError(index: number, errorCode: string): boolean {
    const control = this.sources.at(index);
    return this.submitAttempted() && control.hasError(errorCode);
  }

  protected sourceLengthErrorMessage(index: number): string | null {
    const source = this.sources.at(index);
    const fields = [
      {
        name: 'journal',
        label: 'Journal',
        maxLength: this.maxSourceJournalLength,
      },
      { name: 'author', label: 'Author', maxLength: this.maxSourceAuthorLength },
      { name: 'title', label: 'Title', maxLength: this.maxSourceTitleLength },
      { name: 'url', label: 'URL', maxLength: this.maxSourceUrlLength },
    ] as const;
    const invalidField = fields.find(field =>
      source.get(field.name)?.hasError('maxlength'),
    );

    if (!invalidField) {
      return null;
    }

    return `${invalidField.label} must not be longer than ${invalidField.maxLength} characters.`;
  }

  protected hasCaptionError(errorCode: string): boolean {
    return (
      this.submitAttempted()
      && this.postForm.controls.caption.hasError(errorCode)
    );
  }

  protected captionLength(): number {
    return this.postForm.controls.caption.value.length;
  }

  protected topicOptionLabel(topic: TopicOption): string {
    return topic.label;
  }

  protected topicLabelById(topicId: string): string {
    return (
      this.topicOptions().find(option => option.value === topicId)?.label ?? topicId
    );
  }

  protected openPost(post: PostResponse) {
    this.selectedPost.set(post);
    this.sourcesExpanded.set(false);
  }

  protected closePost() {
    this.selectedPost.set(null);
    this.sourcesExpanded.set(false);
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

        if (this.selectedPost()?.postId === post.postId) {
          this.closePost();
        }

        this.postPendingDeletion.set(null);
        this.deletingPost.set(false);
        this.notificationService.showSuccess('Post deleted.');
      },
      error: () => {
        this.deletingPost.set(false);
      },
    });
  }

  protected toggleSources() {
    this.sourcesExpanded.update(expanded => !expanded);
  }

  protected sourcesButtonLabel(post: PostResponse): string {
    const sourceCount = this.postSources(post).length;
    const sourceLabel = sourceCount === 1 ? 'source' : 'sources';
    return this.sourcesExpanded()
      ? 'Hide sources'
      : `Sources (${sourceCount} ${sourceLabel})`;
  }

  protected postMediaUrl(post: PostResponse | null): string | null {
    if (!post) {
      return null;
    }

    return post.mediaUrl;
  }

  protected postMediaKind(post: PostResponse): PreviewKind {
    const mediaUrl = post.mediaRef ?? post.mediaUrl;

    if (!mediaUrl) {
      return null;
    }

    return /\.(mp4|webm)$/i.test(mediaUrl) ? 'video' : 'image';
  }

  protected formattedCommentDate(commentDate: string): string {
    return this.formatDateTime(commentDate);
  }

  protected formattedSourceDate(source: SingleSourceResponse): string {
    if (!source.date) {
      return '';
    }

    return new Intl.DateTimeFormat('de-AT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(new Date(source.date));
  }

  protected postSources(post: PostResponse): SingleSourceResponse[] {
    return post.source?.singleSources ?? [];
  }

  protected displaySourceUrl(url: string): string {
    const maxLength = 288;

    if (url.length <= maxLength) {
      return url;
    }

    return `${url.slice(0, maxLength - 1)}…`;
  }

  protected get sources(): FormArray {
    return this.postForm.controls.sources;
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

  private formatDateTime(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return '';
    }

    return new Intl.DateTimeFormat('de-AT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
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

  private buildRequest(): CreatePostRequest {
    const rawValue = this.postForm.getRawValue();
    return {
      caption: rawValue.caption.trim(),
      mediaRef: null,
      isPublic: this.isVerifiedUser() ? rawValue.isPublic : false,
      isAiGenerated: rawValue.isAiGenerated,
      topicIds: rawValue.topicIds,
      source: this.buildSourceRequest(rawValue.sources),
    };
  }

  private createPostWithMediaRef(mediaRef: string | null) {
    const request = {
      ...this.buildRequest(),
      mediaRef,
    };

    this.postService.createPost(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.notificationService.showSuccess('Post created successfully.');
        this.closeCreateDialog();
        this.loadMyPosts();
      },
      error: () => {
        this.submitting.set(false);
      },
    });
  }

  private buildSourceRequest(
    rawSources: SourceFormValue[],
  ): CreateSourceRequest | null {
    const singleSources = rawSources
      .map(source => this.mapSource(source))
      .filter((source): source is CreateSingleSourceRequest => source !== null);

    if (singleSources.length === 0) {
      return null;
    }

    return {
      upvotes: 0,
      downvotes: 0,
      singleSources,
    };
  }

  private mapSource(source: SourceFormValue): CreateSingleSourceRequest | null {
    const normalizedSource = {
      journal: this.normalizeOptionalValue(source.journal),
      author: this.normalizeOptionalValue(source.author),
      title: this.normalizeOptionalValue(source.title),
      url: this.normalizeOptionalValue(source.url),
      date: this.normalizeOptionalDate(source.date),
    };

    const hasContent = Object.values(normalizedSource).some(
      value => value !== null,
    );

    if (!hasContent) {
      return null;
    }

    return normalizedSource;
  }

  private normalizeOptionalValue(value: string): string | null {
    const normalizedValue = value.trim();
    return normalizedValue === '' ? null : normalizedValue;
  }

  private normalizeOptionalDate(value: string | Date | null): string | null {
    if (value === null) {
      return null;
    }

    if (value instanceof Date) {
      return Number.isNaN(value.getTime())
        ? null
        : value.toISOString().slice(0, 10);
    }

    return this.normalizeOptionalValue(value);
  }

  private createSourceGroup() {
    return this.fb.nonNullable.group(
      {
        journal: this.fb.nonNullable.control('', [
          Validators.maxLength(this.maxSourceJournalLength),
        ]),
        author: this.fb.nonNullable.control('', [
          Validators.maxLength(this.maxSourceAuthorLength),
        ]),
        title: this.fb.nonNullable.control('', [
          Validators.maxLength(this.maxSourceTitleLength),
        ]),
        url: this.fb.nonNullable.control('', [
          Validators.maxLength(this.maxSourceUrlLength),
        ]),
        date: '',
      },
      { validators: this.sourceValidator() },
    );
  }

  private notBlankValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null =>
      this.isBlankValue(control.value) ? { blank: true } : null;
  }

  private sourceValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const source = control.value as SourceFormValue;
      const hasContent = [
        source.journal,
        source.author,
        source.title,
        source.url,
        source.date,
      ].some(value => !this.isBlankValue(value));

      if (!hasContent) {
        return null;
      }

      const errors: ValidationErrors = {};

      if (this.isBlankValue(source.title)) {
        errors['sourceTitleRequired'] = true;
      }

      if (this.isBlankValue(source.author) && this.isBlankValue(source.url)) {
        errors['sourceAuthorOrUrlRequired'] = true;
      }

      return Object.keys(errors).length === 0 ? null : errors;
    };
  }

  private isBlankValue(value: unknown): boolean {
    return typeof value === 'string' ? value.trim() === '' : value == null;
  }

  private toTopicOption(topic: TopicResponse): TopicOption {
    return {
      label: `${topic.emoji} ${topic.name}`,
      value: topic.topicId,
    };
  }

  private resolvePreviewKind(file: File): PreviewKind {
    if (file.type.startsWith('image/')) {
      return 'image';
    }

    if (file.type.startsWith('video/')) {
      return 'video';
    }

    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension && ['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(extension)) {
      return 'image';
    }

    if (extension && ['mp4', 'webm', 'mov'].includes(extension)) {
      return 'video';
    }

    return null;
  }

  private isValidPostMedia(file: File): boolean {
    if (file.size > MAX_POST_MEDIA_SIZE_BYTES) {
      this.notificationService.showWarning(
        `Please choose an image or video smaller than ${MAX_POST_MEDIA_SIZE_LABEL}.`,
      );
      return false;
    }

    if (!this.isAllowedPostMediaType(file)) {
      this.notificationService.showWarning(
        `Please choose a supported media file: ${ACCEPTED_POST_MEDIA_FORMATS_LABEL}.`,
      );
      return false;
    }

    return true;
  }

  private isAllowedPostMediaType(file: File): boolean {
    if (ALLOWED_POST_MEDIA_TYPES.has(file.type)) {
      return true;
    }

    const extension = file.name.split('.').pop()?.toLowerCase();
    return extension ? ALLOWED_POST_MEDIA_EXTENSIONS.has(extension) : false;
  }

  private clearSelectedMedia() {
    this.revokePreview();
    this.selectedMediaFile.set(null);
    this.selectedFileName.set(null);
    this.previewKind.set(null);
  }

  private resetFormState() {
    this.clearSelectedMedia();
    this.submitAttempted.set(false);
    this.postForm.reset({
      caption: '',
      isPublic: false,
      isAiGenerated: false,
      topicIds: [],
    });
    this.sources.clear();
    this.sources.push(this.createSourceGroup());
    this.expandedSourceIndex.set(0);

    const inputElement = this.mediaInput()?.nativeElement;
    if (inputElement) {
      inputElement.value = '';
    }
  }

  private revokePreview() {
    const currentPreviewUrl = this.previewUrl();
    if (currentPreviewUrl) {
      URL.revokeObjectURL(currentPreviewUrl);
      this.previewUrl.set(null);
    }
  }

  private revokeAvatarPreview() {
    const currentAvatarPreviewUrl = this.avatarPreviewUrl();
    if (currentAvatarPreviewUrl) {
      URL.revokeObjectURL(currentAvatarPreviewUrl);
      this.avatarPreviewUrl.set(null);
    }
  }
}

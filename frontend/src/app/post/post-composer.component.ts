import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { MultiSelect } from 'primeng/multiselect';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';

import { NotificationService } from '../core/services/notification.service';
import { FileService } from './file.service';
import {
  buildCreatePostRequest,
  collapsedSourceTitle,
  createPostForm,
  createSourceGroup,
  MAX_CAPTION_LENGTH,
  MAX_SOURCE_AUTHOR_LENGTH,
  MAX_SOURCE_JOURNAL_LENGTH,
  MAX_SOURCE_TITLE_LENGTH,
  MAX_SOURCE_URL_LENGTH,
  MAX_SOURCES,
  PostFormRawValue,
  sourceLengthErrorMessage,
} from './post-form.utils';
import {
  ACCEPTED_POST_MEDIA_FORMATS_LABEL,
  ACCEPTED_POST_MEDIA_INPUT_TYPES,
  MAX_POST_MEDIA_SIZE_LABEL,
  isLongUploadPostVideo,
  postMediaValidationMessage,
  resolvePreviewKind,
} from './post-media.utils';
import { PreviewKind, SourceFormValue, TopicOption } from './post-page.models';
import { PostService } from './post.service';

@Component({
  selector: 'app-post-composer',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    FloatLabel,
    InputText,
    MultiSelect,
    Textarea,
    ToggleSwitch,
  ],
  templateUrl: './post-composer.component.html',
  styleUrl: './post-composer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostComposerComponent implements OnDestroy {
  @Input({ required: true }) visible = false;
  @Input({ required: true }) isVerifiedUser = false;
  @Input({ required: true }) topicOptions: TopicOption[] = [];
  @Input({ required: true }) loadingTopics = false;
  @Input() modal = true;
  @Input() dismissableMask = true;

  @Output() readonly visibleChange = new EventEmitter<boolean>();
  @Output() readonly created = new EventEmitter<void>();

  protected readonly maxCaptionLength = MAX_CAPTION_LENGTH;
  protected readonly maxSources = MAX_SOURCES;
  protected readonly maxSourceJournalLength = MAX_SOURCE_JOURNAL_LENGTH;
  protected readonly maxSourceAuthorLength = MAX_SOURCE_AUTHOR_LENGTH;
  protected readonly maxSourceTitleLength = MAX_SOURCE_TITLE_LENGTH;
  protected readonly maxSourceUrlLength = MAX_SOURCE_URL_LENGTH;
  protected readonly maxPostMediaSizeLabel = MAX_POST_MEDIA_SIZE_LABEL;
  protected readonly acceptedPostMediaFormatsLabel =
    ACCEPTED_POST_MEDIA_FORMATS_LABEL;
  protected readonly acceptedPostMediaInputTypes =
    ACCEPTED_POST_MEDIA_INPUT_TYPES;

  private readonly fb = inject(FormBuilder);
  private readonly postService = inject(PostService);
  private readonly fileService = inject(FileService);
  private readonly notificationService = inject(NotificationService);
  private readonly mediaInput =
    viewChild<ElementRef<HTMLInputElement>>('mediaInput');

  protected readonly currentStep = signal(1);
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly previewKind = signal<PreviewKind>(null);
  protected readonly selectedFileName = signal<string | null>(null);
  protected readonly selectedMediaFile = signal<File | null>(null);
  protected readonly uploadedMediaRef = signal<string | null>(null);
  protected readonly uploadingMedia = signal(false);
  protected readonly submitting = signal(false);
  protected readonly waitingForMediaBeforeSubmit = signal(false);
  protected readonly submitAttempted = signal(false);
  protected readonly expandedSourceIndex = signal<number | null>(0);
  protected readonly postForm = createPostForm(this.fb);
  protected readonly selectedMediaMayTakeLonger = computed(() =>
    isLongUploadPostVideo(this.selectedMediaFile()),
  );
  protected readonly continueButtonLabel = computed(() =>
    'Weiter',
  );
  protected readonly submitButtonLabel = computed(() =>
    this.waitingForMediaBeforeSubmit()
      ? 'Uploading media...'
      : this.submitting()
        ? 'Creating post...'
        : 'Create Post',
  );
  private mediaUploadRequestId = 0;

  ngOnDestroy() {
    this.mediaUploadRequestId++;
    this.deleteUploadedMediaIfNeeded();
    this.revokePreview();
  }

  protected openMediaPicker() {
    this.mediaInput()?.nativeElement.click();
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
    this.deleteUploadedMediaIfNeeded();
    this.selectedMediaFile.set(file);
    this.selectedFileName.set(file.name);
    this.uploadedMediaRef.set(null);
    this.uploadingMedia.set(false);
    this.mediaUploadRequestId++;
    this.previewUrl.set(URL.createObjectURL(file));
    this.previewKind.set(resolvePreviewKind(file));
  }

  protected continueToDetails() {
    const media = this.selectedMediaFile();

    if (media && !this.uploadedMediaRef() && !this.uploadingMedia()) {
      this.startMediaUpload(media);
    }

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

    this.sources.push(createSourceGroup(this.fb));
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
    return collapsedSourceTitle(source);
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

    this.submitting.set(true);
    const media = this.selectedMediaFile();
    const mediaRef = this.uploadedMediaRef();

    if (!media || mediaRef) {
      this.createPostWithMediaRef(mediaRef);
      return;
    }

    if (this.uploadingMedia()) {
      this.waitingForMediaBeforeSubmit.set(true);
      return;
    }

    if (!this.isValidPostMedia(media)) {
      this.submitting.set(false);
      this.currentStep.set(1);
      this.clearSelectedMedia();
      return;
    }

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
    return sourceLengthErrorMessage(this.sources.at(index));
  }

  protected removeSelectedMedia() {
    this.clearSelectedMedia();

    const inputElement = this.mediaInput()?.nativeElement;
    if (inputElement) {
      inputElement.value = '';
    }
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

  protected isPublicPost(): boolean {
    return this.postForm.controls.isPublic.value;
  }

  protected topicOptionLabel(topic: TopicOption): string {
    return topic.label;
  }

  protected selectedTopicsSummary(): string {
    const selectedValues = this.postForm.controls.topicIds.value;
    return this.topicOptions
      .filter(option => selectedValues.includes(option.value))
      .map(option => option.label)
      .join(', ');
  }

  protected get sources(): FormArray {
    return this.postForm.controls.sources;
  }

  protected onVisibleChange(nextVisible: boolean) {
    this.visibleChange.emit(nextVisible);

    if (!nextVisible) {
      this.resetFormState();
    }
  }

  protected onHide() {
    this.visibleChange.emit(false);
    this.resetFormState();
  }

  private createPostWithMediaRef(mediaRef: string | null) {
    const request = buildCreatePostRequest(
      this.postForm.getRawValue() as PostFormRawValue,
      this.isVerifiedUser,
      mediaRef,
    );

    this.postService.createPost(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.waitingForMediaBeforeSubmit.set(false);
        this.notificationService.showSuccess('Post created successfully.');
        this.created.emit();
        this.visibleChange.emit(false);
        this.resetFormState(false);
      },
      error: () => {
        this.submitting.set(false);
        this.waitingForMediaBeforeSubmit.set(false);
      },
    });
  }

  private isValidPostMedia(file: File): boolean {
    const validationMessage = postMediaValidationMessage(file);
    if (validationMessage) {
      this.notificationService.showWarning(validationMessage);
      return false;
    }

    return true;
  }

  private clearSelectedMedia() {
    this.revokePreview();
    this.deleteUploadedMediaIfNeeded();
    this.selectedMediaFile.set(null);
    this.selectedFileName.set(null);
    this.uploadedMediaRef.set(null);
    this.uploadingMedia.set(false);
    this.waitingForMediaBeforeSubmit.set(false);
    this.mediaUploadRequestId++;
    this.previewKind.set(null);
  }

  private deleteUploadedMediaIfNeeded() {
    const mediaRef = this.uploadedMediaRef();
    if (!mediaRef) {
      return;
    }

    this.fileService.deletePostMedia(mediaRef).subscribe({
      error: () => {
        this.notificationService.showWarning(
          'The previously uploaded media could not be removed.',
        );
      },
    });
  }

  private startMediaUpload(media: File) {
    if (!this.isValidPostMedia(media)) {
      this.clearSelectedMedia();
      return;
    }

    const requestId = ++this.mediaUploadRequestId;
    this.uploadingMedia.set(true);

    this.fileService.uploadPostMedia(media).subscribe({
      next: uploadedFile => {
        if (requestId !== this.mediaUploadRequestId) {
          this.fileService.deletePostMedia(uploadedFile.mediaRef).subscribe();
          return;
        }

        this.uploadedMediaRef.set(uploadedFile.mediaRef);
        this.uploadingMedia.set(false);
        if (this.waitingForMediaBeforeSubmit()) {
          this.waitingForMediaBeforeSubmit.set(false);
          this.createPostWithMediaRef(uploadedFile.mediaRef);
        }
      },
      error: () => {
        if (requestId === this.mediaUploadRequestId) {
          this.uploadingMedia.set(false);
          this.submitting.set(false);
          this.waitingForMediaBeforeSubmit.set(false);
        }
      },
    });
  }

  private resetFormState(deleteUploadedMedia = true) {
    this.currentStep.set(1);
    if (deleteUploadedMedia) {
      this.clearSelectedMedia();
    } else {
      this.revokePreview();
      this.selectedMediaFile.set(null);
      this.selectedFileName.set(null);
      this.uploadedMediaRef.set(null);
      this.uploadingMedia.set(false);
      this.waitingForMediaBeforeSubmit.set(false);
      this.mediaUploadRequestId++;
      this.previewKind.set(null);
    }
    this.uploadingMedia.set(false);
    this.waitingForMediaBeforeSubmit.set(false);
    this.submitAttempted.set(false);
    this.postForm.reset({
      caption: '',
      isPublic: false,
      isAiGenerated: false,
      topicIds: [],
    });
    this.sources.clear();
    this.sources.push(createSourceGroup(this.fb));
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
}

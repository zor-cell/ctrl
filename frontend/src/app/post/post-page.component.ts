import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
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
import { TopicResponse } from './dto/TopicResponse';
import { PostService } from './post.service';

interface TopicOption {
  label: string;
  value: string;
}

type PreviewKind = 'image' | 'video' | null;

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
export class PostPageComponent implements OnInit {
  protected readonly maxCaptionLength = 10000;
  protected readonly maxSources = 10;
  private readonly fb = inject(FormBuilder);
  private readonly postService = inject(PostService);
  private readonly notificationService = inject(NotificationService);
  protected readonly userService = inject(UserService);
  private readonly mediaInput =
    viewChild<ElementRef<HTMLInputElement>>('mediaInput');

  protected readonly dialogVisible = signal(false);
  protected readonly currentStep = signal(1);
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly previewKind = signal<PreviewKind>(null);
  protected readonly selectedFileName = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly loadingTopics = signal(false);
  protected readonly submitAttempted = signal(false);
  protected readonly expandedSourceIndex = signal<number | null>(0);
  protected readonly topicOptions = signal<TopicOption[]>([]);
  protected readonly isVerifiedUser = computed(
    () => this.userService.user()?.isVerified ?? false,
  );
  protected readonly canContinueToDetails = computed(
    () => this.selectedFileName() !== null,
  );
  protected readonly selectedTopicsSummary = computed(() => {
    const selectedValues = this.postForm.controls.topicIds.value;
    return this.topicOptions()
      .filter(option => selectedValues.includes(option.value))
      .map(option => option.label)
      .join(', ');
  });
  protected readonly captionLength = computed(
    () => this.postForm.controls.caption.value.length,
  );

  protected readonly postForm = this.fb.group({
    caption: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(this.maxCaptionLength),
    ]),
    isPublic: this.fb.nonNullable.control(false),
    isAiGenerated: this.fb.nonNullable.control(false),
    topicIds: this.fb.nonNullable.control<string[]>([]),
    sources: this.fb.array([this.createSourceGroup()]),
  });

  ngOnInit() {
    this.loadTopics();
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

  protected openMediaPicker() {
    this.mediaInput()?.nativeElement.click();
  }

  protected onMediaSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);

    if (!file) {
      return;
    }

    this.revokePreview();
    this.selectedFileName.set(file.name);
    this.previewUrl.set(URL.createObjectURL(file));
    this.previewKind.set(this.resolvePreviewKind(file.type));
  }

  protected continueToDetails() {
    if (this.canContinueToDetails()) {
      this.currentStep.set(2);
    }
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
    return this.submitAttempted() && !this.isSourceExpanded(index) && this.sources.at(index).invalid;
  }

  protected submitPost() {
    this.submitAttempted.set(true);

    if (this.postForm.invalid) {
      this.postForm.markAllAsTouched();
      return;
    }

    const request = this.buildRequest();
    this.submitting.set(true);

    this.postService.createPost(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.notificationService.showSuccess('Post created successfully.');
        this.closeCreateDialog();
      },
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

  protected hasCaptionError(errorCode: string): boolean {
    return this.submitAttempted() && this.postForm.controls.caption.hasError(errorCode);
  }

  protected topicOptionLabel(topic: TopicOption): string {
    return topic.label;
  }

  protected get sources(): FormArray {
    return this.postForm.controls.sources;
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
      caption: rawValue.caption,
      mediaRef: this.selectedFileName(),
      isPublic: this.isVerifiedUser() ? rawValue.isPublic : false,
      isAiGenerated: rawValue.isAiGenerated,
      topicIds: rawValue.topicIds,
      source: this.buildSourceRequest(rawValue.sources),
    };
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

    const hasContent = Object.values(normalizedSource).some(value => value !== null);

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
      return Number.isNaN(value.getTime()) ? null : value.toISOString().slice(0, 10);
    }

    return this.normalizeOptionalValue(value);
  }

  private createSourceGroup() {
    return this.fb.nonNullable.group(
      {
        journal: '',
        author: '',
        title: '',
        url: '',
        date: '',
      },
      { validators: this.sourceValidator() },
    );
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

  private resolvePreviewKind(mimeType: string): PreviewKind {
    if (mimeType.startsWith('image/')) {
      return 'image';
    }

    if (mimeType.startsWith('video/')) {
      return 'video';
    }

    return null;
  }

  private resetFormState() {
    this.revokePreview();
    this.selectedFileName.set(null);
    this.previewKind.set(null);
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
}

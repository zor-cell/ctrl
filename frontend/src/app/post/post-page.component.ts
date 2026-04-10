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
import { LabelResponse } from './dto/LabelResponse';
import { PostService } from './post.service';

interface LabelOption {
  label: string;
  value: string;
}

type PreviewKind = 'image' | 'video' | null;

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
  protected readonly loadingLabels = signal(false);
  protected readonly labelOptions = signal<LabelOption[]>([]);
  protected readonly isVerifiedUser = computed(
    () => this.userService.user()?.isVerified ?? false,
  );
  protected readonly canContinueToDetails = computed(
    () => this.selectedFileName() !== null,
  );
  protected readonly selectedLabelsSummary = computed(() => {
    const selectedValues = this.postForm.controls.labelIds.value;
    return this.labelOptions()
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
    labelIds: this.fb.nonNullable.control<string[]>([]),
    sources: this.fb.array([this.createSourceGroup()]),
  });

  ngOnInit() {
    this.loadLabels();
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

  protected addSource() {
    this.sources.push(this.createSourceGroup());
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
      return;
    }

    this.sources.removeAt(index);
  }

  protected submitPost() {
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

  protected get sources(): FormArray {
    return this.postForm.controls.sources;
  }

  private loadLabels() {
    this.loadingLabels.set(true);
    this.postService.getLabels().subscribe({
      next: labels => {
        this.labelOptions.set(labels.map(label => this.toLabelOption(label)));
        this.loadingLabels.set(false);
      },
      error: () => {
        this.labelOptions.set([]);
        this.loadingLabels.set(false);
        this.notificationService.showWarning(
          'Labels could not be loaded from the backend.',
          'Labels unavailable',
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
      labelIds: rawValue.labelIds,
      source: this.buildSourceRequest(rawValue.sources),
    };
  }

  private buildSourceRequest(rawSources: {
    journal: string;
    author: string;
    title: string;
    url: string;
    date: string;
  }[]): CreateSourceRequest | null {
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

  private mapSource(source: {
    journal: string;
    author: string;
    title: string;
    url: string;
    date: string;
  }): CreateSingleSourceRequest | null {
    const normalizedSource = {
      journal: this.normalizeOptionalValue(source.journal),
      author: this.normalizeOptionalValue(source.author),
      title: this.normalizeOptionalValue(source.title),
      url: this.normalizeOptionalValue(source.url),
      date: this.normalizeOptionalValue(source.date),
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

  private createSourceGroup() {
    return this.fb.nonNullable.group({
      journal: '',
      author: '',
      title: '',
      url: '',
      date: '',
    });
  }

  private toLabelOption(label: LabelResponse): LabelOption {
    return {
      label: `${label.emoji} ${label.name}`,
      value: label.labelId,
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
    this.postForm.reset({
      caption: '',
      isPublic: false,
      labelIds: [],
    });
    this.sources.clear();
    this.sources.push(this.createSourceGroup());

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

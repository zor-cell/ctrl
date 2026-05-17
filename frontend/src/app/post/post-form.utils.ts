import {
  AbstractControl,
  FormBuilder,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';

import { CreatePostRequest } from './dto/CreatePostRequest';
import { CreateSingleSourceRequest } from './dto/CreateSingleSourceRequest';
import { CreateSourceRequest } from './dto/CreateSourceRequest';
import { SourceFormValue } from './post-page.models';

export const MAX_CAPTION_LENGTH = 10000;
export const MAX_SOURCES = 10;
export const MAX_SOURCE_JOURNAL_LENGTH = 255;
export const MAX_SOURCE_AUTHOR_LENGTH = 255;
export const MAX_SOURCE_TITLE_LENGTH = 500;
export const MAX_SOURCE_URL_LENGTH = 2048;

export interface PostFormRawValue {
  caption: string;
  isPublic: boolean;
  isAiGenerated: boolean;
  topicIds: string[];
  sources: SourceFormValue[];
}

export function createPostForm(fb: FormBuilder) {
  return fb.group({
    caption: fb.nonNullable.control('', [
      Validators.required,
      notBlankValidator(),
      Validators.maxLength(MAX_CAPTION_LENGTH),
    ]),
    isPublic: fb.nonNullable.control(false),
    isAiGenerated: fb.nonNullable.control(false),
    topicIds: fb.nonNullable.control<string[]>([]),
    sources: fb.array([createSourceGroup(fb)]),
  });
}

export function createSourceGroup(fb: FormBuilder) {
  return fb.nonNullable.group(
    {
      journal: fb.nonNullable.control('', [
        Validators.maxLength(MAX_SOURCE_JOURNAL_LENGTH),
      ]),
      author: fb.nonNullable.control('', [
        Validators.maxLength(MAX_SOURCE_AUTHOR_LENGTH),
      ]),
      title: fb.nonNullable.control('', [
        Validators.maxLength(MAX_SOURCE_TITLE_LENGTH),
      ]),
      url: fb.nonNullable.control('', [
        Validators.maxLength(MAX_SOURCE_URL_LENGTH),
      ]),
      date: '',
    },
    { validators: sourceValidator() },
  );
}

export function buildCreatePostRequest(
  rawValue: PostFormRawValue,
  isVerifiedUser: boolean,
  mediaRef: string | null,
): CreatePostRequest {
  return {
    caption: rawValue.caption.trim(),
    mediaRef,
    isPublic: isVerifiedUser ? rawValue.isPublic : false,
    isAiGenerated: rawValue.isAiGenerated,
    topicIds: rawValue.topicIds,
    source: buildSourceRequest(rawValue.sources),
  };
}

export function collapsedSourceTitle(source: SourceFormValue): string {
  const title = source.title.trim();
  return title === '' ? 'Untitled source' : title;
}

export function sourceLengthErrorMessage(source: AbstractControl): string | null {
  const fields = [
    {
      name: 'journal',
      label: 'Journal',
      maxLength: MAX_SOURCE_JOURNAL_LENGTH,
    },
    { name: 'author', label: 'Author', maxLength: MAX_SOURCE_AUTHOR_LENGTH },
    { name: 'title', label: 'Title', maxLength: MAX_SOURCE_TITLE_LENGTH },
    { name: 'url', label: 'URL', maxLength: MAX_SOURCE_URL_LENGTH },
  ] as const;
  const invalidField = fields.find(field =>
    source.get(field.name)?.hasError('maxlength'),
  );

  if (!invalidField) {
    return null;
  }

  return `${invalidField.label} must not be longer than ${invalidField.maxLength} characters.`;
}

function buildSourceRequest(
  rawSources: SourceFormValue[],
): CreateSourceRequest | null {
  const singleSources = rawSources
    .map(source => mapSource(source))
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

function mapSource(source: SourceFormValue): CreateSingleSourceRequest | null {
  const normalizedSource = {
    journal: normalizeOptionalValue(source.journal),
    author: normalizeOptionalValue(source.author),
    title: normalizeOptionalValue(source.title),
    url: normalizeOptionalValue(source.url),
    date: normalizeOptionalDate(source.date),
  };

  const hasContent = Object.values(normalizedSource).some(
    value => value !== null,
  );

  if (!hasContent) {
    return null;
  }

  return normalizedSource;
}

function notBlankValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (value == null || value === '') {
      return null;
    }

    return isBlankValue(value) ? { blank: true } : null;
  };
}

function sourceValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const source = control.value as SourceFormValue;
    const hasContent = [
      source.journal,
      source.author,
      source.title,
      source.url,
      source.date,
    ].some(value => !isBlankValue(value));

    if (!hasContent) {
      return null;
    }

    const errors: ValidationErrors = {};

    if (isBlankValue(source.title)) {
      errors['sourceTitleRequired'] = true;
    }

    if (isBlankValue(source.author) && isBlankValue(source.url)) {
      errors['sourceAuthorOrUrlRequired'] = true;
    }

    return Object.keys(errors).length === 0 ? null : errors;
  };
}

function normalizeOptionalValue(value: string): string | null {
  const normalizedValue = value.trim();
  return normalizedValue === '' ? null : normalizedValue;
}

function normalizeOptionalDate(value: string | Date | null): string | null {
  if (value === null) {
    return null;
  }

  if (value instanceof Date) {
    return Number.isNaN(value.getTime())
      ? null
      : value.toISOString().slice(0, 10);
  }

  return normalizeOptionalValue(value);
}

function isBlankValue(value: unknown): boolean {
  return typeof value === 'string' ? value.trim() === '' : value == null;
}

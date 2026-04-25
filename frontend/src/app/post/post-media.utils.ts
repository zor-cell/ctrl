import { PreviewKind } from './post-page.models';

export const MAX_POST_MEDIA_SIZE_BYTES = 100 * 1024 * 1024;
export const MAX_POST_MEDIA_SIZE_LABEL = '100 MB';
export const ACCEPTED_POST_MEDIA_FORMATS_LABEL =
  'JPG, PNG, WEBP, GIF, MP4, WEBM, MOV';
export const ACCEPTED_POST_MEDIA_INPUT_TYPES = [
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

export function resolvePreviewKind(file: File): PreviewKind {
  if (file.type.startsWith('image/')) {
    return 'image';
  }

  if (file.type.startsWith('video/')) {
    return 'video';
  }

  const extension = file.name.split('.').pop()?.toLowerCase();
  return mediaKindFromExtension(extension);
}

export function postMediaKind(mediaUrl: string | null | undefined): PreviewKind {
  if (!mediaUrl) {
    return null;
  }

  return /\.(mp4|webm|mov)$/i.test(mediaUrl) ? 'video' : 'image';
}

export function postMediaValidationMessage(file: File): string | null {
  if (file.size > MAX_POST_MEDIA_SIZE_BYTES) {
    return `Please choose an image or video smaller than ${MAX_POST_MEDIA_SIZE_LABEL}.`;
  }

  if (!isAllowedPostMediaType(file)) {
    return `Please choose a supported media file: ${ACCEPTED_POST_MEDIA_FORMATS_LABEL}.`;
  }

  return null;
}

export function isLongUploadPostVideo(file: File | null): boolean {
  if (!file) {
    return false;
  }

  const extension = file.name.split('.').pop()?.toLowerCase();
  return (
    file.type === 'video/quicktime'
    || file.type === 'video/x-quicktime'
    || extension === 'mov'
  );
}

function isAllowedPostMediaType(file: File): boolean {
  if (ALLOWED_POST_MEDIA_TYPES.has(file.type)) {
    return true;
  }

  const extension = file.name.split('.').pop()?.toLowerCase();
  return extension ? ALLOWED_POST_MEDIA_EXTENSIONS.has(extension) : false;
}

function mediaKindFromExtension(extension: string | undefined): PreviewKind {
  if (!extension) {
    return null;
  }

  if (['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(extension)) {
    return 'image';
  }

  if (['mp4', 'webm', 'mov'].includes(extension)) {
    return 'video';
  }

  return null;
}

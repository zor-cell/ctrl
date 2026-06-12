export function previewMediaUrl(url: string | null | undefined): string | null {
  if (!url || isVideoUrl(url)) {
    return url ?? null;
  }

  try {
    const parsedUrl = new URL(url);
    if (
      parsedUrl.pathname.includes('/media_attachments/files/')
      && parsedUrl.pathname.includes('/original/')
    ) {
      parsedUrl.pathname = parsedUrl.pathname.replace('/original/', '/small/');
      return parsedUrl.toString();
    }
  } catch {
    return url;
  }

  return url;
}

function isVideoUrl(url: string): boolean {
  return /\.(mp4|webm|ogg|mov)(?:[?#].*)?$/i.test(url);
}

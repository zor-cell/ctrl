import { Injectable } from '@angular/core';

export type MediaCacheKind = 'image' | 'video';

@Injectable({
  providedIn: 'root',
})
export class MediaCacheService {
  private readonly cachedUrls = new Set<string>();
  private readonly pendingUrls = new Set<string>();
  private readonly maxCachedUrls = 120;

  cacheUrl(url: string | null | undefined, kind?: MediaCacheKind | null): void {
    if (!this.canCache(url)) {
      return;
    }

    if (this.cachedUrls.has(url) || this.pendingUrls.has(url)) {
      return;
    }

    this.pendingUrls.add(url);

    if ((kind ?? this.inferKind(url)) === 'video') {
      this.preloadVideoMetadata(url);
      return;
    }

    this.preloadImage(url);
  }

  cacheUrls(urls: (string | null | undefined)[]): void {
    urls.forEach(url => this.cacheUrl(url));
  }

  private canCache(url: string | null | undefined): url is string {
    return typeof url === 'string'
      && url.length > 0
      && !url.startsWith('blob:')
      && !url.startsWith('data:')
      && typeof window !== 'undefined';
  }

  private rememberUrl(url: string): void {
    this.pendingUrls.delete(url);

    if (this.cachedUrls.size >= this.maxCachedUrls) {
      const oldestUrl = this.cachedUrls.values().next().value;
      if (oldestUrl) {
        this.cachedUrls.delete(oldestUrl);
      }
    }

    this.cachedUrls.add(url);
  }

  private preloadImage(url: string): void {
    const image = new Image();
    image.decoding = 'async';
    image.onload = () => this.rememberUrl(url);
    image.onerror = () => this.pendingUrls.delete(url);
    image.src = url;
  }

  private preloadVideoMetadata(url: string): void {
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.onloadedmetadata = () => this.rememberUrl(url);
    video.onerror = () => this.pendingUrls.delete(url);
    video.src = url;
  }

  private inferKind(url: string): MediaCacheKind {
    return /\.(mp4|webm|mov)(\?|#|$)/i.test(url) ? 'video' : 'image';
  }
}

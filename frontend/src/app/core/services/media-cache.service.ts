import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class MediaCacheService {
  private readonly cachedUrls = new Set<string>();
  private readonly maxCachedUrls = 120;

  cacheUrl(url: string | null | undefined): void {
    if (!this.canCache(url)) {
      return;
    }

    if (this.cachedUrls.has(url)) {
      return;
    }

    this.rememberUrl(url);

    if (this.isVideoUrl(url)) {
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
    image.src = url;
  }

  private preloadVideoMetadata(url: string): void {
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.src = url;
  }

  private isVideoUrl(url: string): boolean {
    return /\.(mp4|webm|mov)(\?|#|$)/i.test(url);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, finalize, map, of, shareReplay } from 'rxjs';

import { environment } from '../../../environments/environment';

export type MediaCacheKind = 'image' | 'video';
export type MediaCachePriority = 'low' | 'normal' | 'high';

interface CachedObjectUrl {
  objectUrl: string;
  expiresAt: number | null;
  priority: MediaCachePriority;
  lastUsedAt: number;
}

interface PresignedMediaUrlResponse {
  mediaUrl: string;
  expiresAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class MediaCacheService {
  private readonly httpClient = inject(HttpClient);

  private readonly cachedUrls = new Set<string>();
  private readonly pendingUrls = new Set<string>();
  private readonly cachedObjectUrls = new Map<string, CachedObjectUrl>();
  private readonly pendingObjectUrls = new Map<string, Observable<string>>();
  private readonly maxCachedUrls = 120;

  resolveMediaUrl(
    url: string | null | undefined,
    kind?: MediaCacheKind | null,
    priority: MediaCachePriority = 'normal',
  ): Observable<string | null> {
    if (!url) {
      return of(null);
    }

    const normalizedUrl = this.normalizeMediaUrl(url);

    if (!this.isProtectedMediaUrl(normalizedUrl)) {
      this.cacheUrl(normalizedUrl, kind);
      return of(normalizedUrl);
    }

    return this.resolveProtectedMediaUrl(normalizedUrl, priority);
  }

  cacheUrl(
    url: string | null | undefined,
    kind?: MediaCacheKind | null,
    priority: MediaCachePriority = 'normal',
  ): void {
    if (!this.canCache(url)) {
      return;
    }

    const normalizedUrl = this.normalizeMediaUrl(url);

    if (this.isProtectedMediaUrl(normalizedUrl)) {
      this.resolveProtectedMediaUrl(normalizedUrl, priority).subscribe();
      return;
    }

    if (this.cachedUrls.has(normalizedUrl) || this.pendingUrls.has(normalizedUrl)) {
      return;
    }

    this.pendingUrls.add(normalizedUrl);

    if ((kind ?? this.inferKind(normalizedUrl)) === 'video') {
      this.preloadVideoMetadata(normalizedUrl);
      return;
    }

    this.preloadImage(normalizedUrl);
  }

  cacheUrls(
    urls: (string | null | undefined)[],
    priority: MediaCachePriority = 'normal',
  ): void {
    urls.forEach(url => this.cacheUrl(url, null, priority));
  }

  clear(): void {
    this.cachedObjectUrls.forEach(entry => this.revokeIfBlobUrl(entry.objectUrl));
    this.cachedObjectUrls.clear();
    this.pendingObjectUrls.clear();
    this.cachedUrls.clear();
    this.pendingUrls.clear();
  }

  private resolveProtectedMediaUrl(url: string, priority: MediaCachePriority): Observable<string> {
    const cachedEntry = this.cachedObjectUrls.get(url);
    if (cachedEntry && !this.isExpired(cachedEntry)) {
      cachedEntry.lastUsedAt = Date.now();
      if (this.priorityRank(priority) > this.priorityRank(cachedEntry.priority)) {
        cachedEntry.priority = priority;
      }
      return of(cachedEntry.objectUrl);
    }

    if (cachedEntry) {
      this.cachedObjectUrls.delete(url);
    }

    const pendingObjectUrl = this.pendingObjectUrls.get(url);
    if (pendingObjectUrl) {
      return pendingObjectUrl;
    }

    const request$ = this.httpClient.get<PresignedMediaUrlResponse>(url).pipe(
      map(response => this.rememberObjectUrl(
        url,
        response.mediaUrl,
        priority,
        Date.parse(response.expiresAt),
      )),
      finalize(() => this.pendingObjectUrls.delete(url)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.pendingObjectUrls.set(url, request$);
    return request$;
  }

  private rememberObjectUrl(
    url: string,
    objectUrl: string,
    priority: MediaCachePriority,
    expiresAt: number | null = null,
  ): string {
    if (this.cachedObjectUrls.size >= this.maxCachedUrls) {
      const evictionUrl = this.findEvictionCandidateUrl();
      if (evictionUrl) {
        const evictionEntry = this.cachedObjectUrls.get(evictionUrl);
        if (evictionEntry) {
          this.revokeIfBlobUrl(evictionEntry.objectUrl);
        }
        this.cachedObjectUrls.delete(evictionUrl);
      }
    }

    this.cachedObjectUrls.set(url, {
      objectUrl,
      expiresAt,
      priority,
      lastUsedAt: Date.now(),
    });
    return objectUrl;
  }

  private isExpired(entry: CachedObjectUrl): boolean {
    if (entry.expiresAt === null || Number.isNaN(entry.expiresAt)) {
      return false;
    }

    return Date.now() > entry.expiresAt - 5000;
  }

  private findEvictionCandidateUrl(): string | null {
    let candidateUrl: string | null = null;
    let candidateEntry: CachedObjectUrl | null = null;

    this.cachedObjectUrls.forEach((entry, url) => {
      if (
        candidateEntry === null
        || this.priorityRank(entry.priority) < this.priorityRank(candidateEntry.priority)
        || (
          this.priorityRank(entry.priority) === this.priorityRank(candidateEntry.priority)
          && entry.lastUsedAt < candidateEntry.lastUsedAt
        )
      ) {
        candidateUrl = url;
        candidateEntry = entry;
      }
    });

    return candidateUrl;
  }

  private priorityRank(priority: MediaCachePriority): number {
    switch (priority) {
      case 'high':
        return 2;
      case 'normal':
        return 1;
      case 'low':
        return 0;
    }
  }

  private isProtectedMediaUrl(url: string): boolean {
    return url.includes('/api/files/media?ref=') || url.includes('/api/files/media-url?ref=');
  }

  isBackendManagedMediaUrl(url: string | null | undefined): boolean {
    if (!url) {
      return false;
    }

    return this.isProtectedMediaUrl(url) || this.extractLegacyGarageMediaRef(url) !== null;
  }

  private normalizeMediaUrl(url: string): string {
    const legacyMediaRef = this.extractLegacyGarageMediaRef(url);
    if (!legacyMediaRef) {
      return url;
    }

    return `${environment.apiUrl}/files/media-url?ref=${encodeURIComponent(legacyMediaRef)}`;
  }

  private extractLegacyGarageMediaRef(url: string): string | null {
    try {
      const parsedUrl = new URL(url);
      const path = parsedUrl.pathname.replace(/^\/+/, '');

      if (parsedUrl.hostname.endsWith('.web.garage.localhost')) {
        return path || null;
      }

      if (parsedUrl.host === 'localhost:3902' || parsedUrl.host === '127.0.0.1:3902') {
        const bucketPrefix = 'ctrl-dev-media/';
        return path.startsWith(bucketPrefix) ? path.slice(bucketPrefix.length) : path || null;
      }

      return null;
    } catch {
      return null;
    }
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

  private revokeIfBlobUrl(url: string): void {
    if (url.startsWith('blob:')) {
      URL.revokeObjectURL(url);
    }
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

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, finalize, map, of, shareReplay, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SILENT_ERROR_CONTEXT } from '../interceptors';

export type MediaCacheKind = 'image' | 'video';
export type MediaCachePriority = 'low' | 'normal' | 'high';

interface CachedObjectUrl {
  objectUrl: string;
  expiresAt: number | null;
  priority: MediaCachePriority;
  lastUsedAt: number;
}

@Injectable({
  providedIn: 'root',
})
export class MediaCacheService {
  private static readonly DEFAULT_CACHE_TTL_MS = 30 * 60 * 1000;

  private readonly httpClient = inject(HttpClient);

  private readonly cachedUrls = new Set<string>();
  private readonly pendingUrls = new Set<string>();
  private readonly cachedObjectUrls = new Map<string, CachedObjectUrl>();
  private readonly pendingObjectUrls = new Map<string, Observable<string>>();
  private readonly refreshScheduled = new Set<string>();
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
    const resolvedKind = kind ?? this.inferKind(normalizedUrl);

    if (!this.isProtectedMediaUrl(normalizedUrl)) {
      this.cacheUrl(normalizedUrl, resolvedKind);
      return of(normalizedUrl);
    }

    return this.resolveProtectedMediaUrl(normalizedUrl, resolvedKind, priority);
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
    const resolvedKind = kind ?? this.inferKind(normalizedUrl);

    if (this.isProtectedMediaUrl(normalizedUrl)) {
      this.resolveProtectedMediaUrl(normalizedUrl, resolvedKind, priority).subscribe();
      return;
    }

    if (this.cachedUrls.has(normalizedUrl) || this.pendingUrls.has(normalizedUrl)) {
      return;
    }

    this.pendingUrls.add(normalizedUrl);

    if (resolvedKind === 'video') {
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
    this.refreshScheduled.clear();
    this.cachedUrls.clear();
    this.pendingUrls.clear();
  }

  private resolveProtectedMediaUrl(
    url: string,
    kind: MediaCacheKind,
    priority: MediaCachePriority,
  ): Observable<string> {
    const cachedEntry = this.cachedObjectUrls.get(url);
    if (cachedEntry) {
      cachedEntry.lastUsedAt = Date.now();
      if (this.priorityRank(priority) > this.priorityRank(cachedEntry.priority)) {
        cachedEntry.priority = priority;
      }

      if (this.isExpired(cachedEntry)) {
        this.scheduleProtectedMediaRefresh(url, kind, priority);
      }

      return of(cachedEntry.objectUrl);
    }

    const pendingObjectUrl = this.pendingObjectUrls.get(url);
    if (pendingObjectUrl) {
      return pendingObjectUrl;
    }

    const request$ = this.fetchProtectedMedia(url, kind, priority).pipe(
      finalize(() => this.pendingObjectUrls.delete(url)),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.pendingObjectUrls.set(url, request$);
    return request$;
  }

  private fetchProtectedMedia(
    url: string,
    kind: MediaCacheKind,
    priority: MediaCachePriority,
  ): Observable<string> {
    const mediaRef = this.extractMediaRef(url);
    if (!mediaRef) {
      return throwError(() => new Error('Invalid media reference'));
    }

    const proxyUrl = `${environment.apiUrl}/files/media?ref=${encodeURIComponent(mediaRef)}`;
    const expiresAt = Date.now() + MediaCacheService.DEFAULT_CACHE_TTL_MS;

    return this.httpClient.get(proxyUrl, {
      responseType: 'blob',
      context: SILENT_ERROR_CONTEXT,
    }).pipe(
      map(blob => this.replaceObjectUrl(
        url,
        URL.createObjectURL(blob),
        priority,
        expiresAt,
      )),
    );
  }

  private extractMediaRef(url: string): string | null {
    if (this.isMediaRef(url)) {
      return url;
    }

    try {
      const parsedUrl = new URL(url, window.location.origin);
      const mediaRef = parsedUrl.searchParams.get('ref');
      return mediaRef ? decodeURIComponent(mediaRef) : null;
    } catch {
      return null;
    }
  }

  private scheduleProtectedMediaRefresh(
    url: string,
    kind: MediaCacheKind,
    priority: MediaCachePriority,
  ): void {
    if (this.pendingObjectUrls.has(url) || this.refreshScheduled.has(url)) {
      return;
    }

    this.refreshScheduled.add(url);
    this.fetchProtectedMedia(url, kind, priority).pipe(
      finalize(() => this.refreshScheduled.delete(url)),
    ).subscribe();
  }

  private replaceObjectUrl(
    url: string,
    objectUrl: string,
    priority: MediaCachePriority,
    expiresAt: number | null = null,
  ): string {
    const previousEntry = this.cachedObjectUrls.get(url);
    if (previousEntry) {
      this.revokeIfBlobUrl(previousEntry.objectUrl);
      this.cachedObjectUrls.delete(url);
    }

    return this.rememberObjectUrl(url, objectUrl, priority, expiresAt);
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

    return Date.now() > entry.expiresAt - 30_000;
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
    return url.includes('/api/files/media?ref=')
      || url.includes('/api/files/media-url?ref=')
      || this.isMediaRef(url);
  }

  isBackendManagedMediaUrl(url: string | null | undefined): boolean {
    if (!url) {
      return false;
    }

    return this.isProtectedMediaUrl(url) || this.extractLegacyGarageMediaRef(url) !== null;
  }

  private normalizeMediaUrl(url: string): string {
    if (this.isMediaRef(url)) {
      return `${environment.apiUrl}/files/media-url?ref=${encodeURIComponent(url)}`;
    }

    const legacyMediaRef = this.extractLegacyGarageMediaRef(url);
    if (!legacyMediaRef) {
      return url;
    }

    return `${environment.apiUrl}/files/media-url?ref=${encodeURIComponent(legacyMediaRef)}`;
  }

  private isMediaRef(url: string): boolean {
    return url.startsWith('users/profile-pictures/')
      || url.startsWith('chats/group-images/')
      || url.startsWith('posts/media/');
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

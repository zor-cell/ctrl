import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ViewEvent} from '../post-view-directive/post-view.directive';

@Injectable({ providedIn: 'root' })
export class ViewTrackingService {
  private readonly http = inject(HttpClient);
  private readonly baseUri = environment.apiUrl;

  private queue: ViewEvent[] = [];
  private flushTimer: ReturnType<typeof setTimeout> | null = null;

  /** Adds a new view event to the queue and schedules the next flush. */
  track(event: ViewEvent): void {
    this.queue.push(event);
    this.scheduleFlush();
  }

  /**
   * Schedules the queue to be sent to the backend.
   * Sends it immediately if ≥5 events have been collected,
   * otherwise sets a 10-second timer (provided one is not already running).
   */
  private scheduleFlush(): void {
    if (this.queue.length >= 5) {
      this.flush();
      return;
    }
    if (!this.flushTimer) {
      this.flushTimer = setTimeout(() => this.flush(), 10_000);
    }
  }

  /**
   * Sends all collected events as a batch to POST /posts/views and clears the queue.
   * Any running timers are reset in the process.
   * If the request fails, the error is logged but not propagated further.
   */
  private flush(): void {
    if (this.flushTimer) {
      clearTimeout(this.flushTimer);
      this.flushTimer = null;
    }
    if (this.queue.length === 0) return;

    const payload = [...this.queue];
    this.queue = [];

    firstValueFrom(
      this.http.post(`${this.baseUri}/posts/views`, { views: payload })
    ).catch(err => console.error('View tracking failed:', err));
  }
}

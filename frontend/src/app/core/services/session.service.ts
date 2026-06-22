import { inject, effect, Injectable } from '@angular/core';
import { AuthService } from '../../auth/auth.service';
import { EMPTY, fromEvent, Observable, Subscription, catchError, switchMap, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { SILENT_ERROR_CONTEXT } from '../interceptors';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly authService = inject(AuthService);
  private readonly httpClient = inject(HttpClient);

  private heartbeat$: Subscription | null = null;
  private readonly INTERVAL_MS = environment.heartbeat.interval;

  constructor() {
    effect((onCleanup) => {
      const authState = this.authService.isAuthenticated();
      if (authState) {
        this.startHeartbeat();
      } else {
        this.stopHeartbeat();
      }

      onCleanup(() => {
        this.stopHeartbeat();
      });
    });

    fromEvent(document, 'visibilitychange').subscribe(() => {
      if (!document.hidden && this.authService.isAuthenticated()) {
        this.startHeartbeat();
      } else {
        this.stopHeartbeat();
      }
    });
  }

  private startHeartbeat(): void {
    if (this.heartbeat$) return;

    if (this.authService.isAuthenticated()) {
      this.heartbeat$ = timer(this.INTERVAL_MS, this.INTERVAL_MS)
        .pipe(
          switchMap(() => this.sendHeartbeat().pipe(
              catchError(() => {
                //dont do anything when error arises
                return EMPTY;
              }))),
        )
        .subscribe();
    }
  }

  private stopHeartbeat(): void {
    this.heartbeat$?.unsubscribe();
    this.heartbeat$ = null;
  }

  private sendHeartbeat(): Observable<void> {
    return this.httpClient.post<void>(
      environment.apiUrl + '/session/heartbeat',
      {},
      {
        context: SILENT_ERROR_CONTEXT,
      },
    );
  }
}

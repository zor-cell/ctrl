import { inject, effect, Injectable } from "@angular/core";
import { AuthService } from "../../auth/auth.service";
import { fromEvent, Observable, retry, Subscription, switchMap, timer } from "rxjs";
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";

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
      })
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
      this.heartbeat$ = timer(this.INTERVAL_MS, this.INTERVAL_MS).pipe(
        switchMap(() => this.sendHeartbeat()),
        retry({ count: 3, delay: 5000 }),
      ).subscribe();
    }
  }

  private stopHeartbeat(): void {
    this.heartbeat$?.unsubscribe();
    this.heartbeat$ = null;
  }

  private sendHeartbeat(): Observable<void> {
    return this.httpClient.post<void>(environment.apiUrl + '/session/heartbeat', {});
  }
}

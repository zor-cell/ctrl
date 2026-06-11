import { SessionConfigUpdateResponse } from "./dto/SessionConfigUpdateResponse";
import { inject, Injectable } from '@angular/core';
import { environment } from "../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { NotificationService } from "../core/services/notification.service";
import { BehaviorSubject, tap } from "rxjs";
import { SessionConfig } from "./dto/SessionConfig";

@Injectable({
  providedIn: 'root',
})
export class SessionConfigService {
  private readonly baseUri: string = environment.apiUrl + '/session';
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);
  private config = new BehaviorSubject<SessionConfig>({ timeLimitMinutes: 0, postLimit: 0, allowSnooze: false });
  config$ = this.config.asObservable();

  public getConfig() {
    return this.httpClient.get<SessionConfig>(this.baseUri + '/config').pipe(
      tap(config => this.config.next(config))
    );
  }

  public updateConfig(updateConfigRequest: SessionConfig) {
    return this.httpClient.put<SessionConfigUpdateResponse>(this.baseUri + '/config', updateConfigRequest).pipe(
      tap(config => {
        if (config.appliedImmediately) {
          this.notificationService.showSuccess('Configuration saved');
        } else {
          this.notificationService.showSuccess('Configuration saved. Changes will take effect tomorrow at midnight');
        }
        this.config.next(config);
      })
    );
  }

  public snooze() {
    return this.httpClient.post<void>(this.baseUri + '/snooze', {});
  }
}

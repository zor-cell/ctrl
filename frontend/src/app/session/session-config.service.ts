import { inject, Injectable } from '@angular/core';
import { environment } from "../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { NotificationService } from "../core/services/notification.service";
import {tap} from "rxjs";
import { SessionConfig } from "./dto/SessionConfig";
import { SessionConfigUpdateResponse } from "./dto/SessionConfigUpdateResponse";
import {Usage} from "./dto/usage";

@Injectable({
  providedIn: 'root',
})
export class SessionConfigService {
  private readonly baseUri: string = environment.apiUrl + '/session';
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);

  public getConfig() {
    return this.httpClient.get<SessionConfig>(this.baseUri + '/config');
  }

  public updateConfig(updateConfigRequest: SessionConfig) {
    return this.httpClient.put<SessionConfigUpdateResponse>(this.baseUri + '/config', updateConfigRequest).pipe(
      tap(config => {
        if (config.appliedImmediately) {
          this.notificationService.showSuccess('Configuration saved');
        } else {
          this.notificationService.showSuccess('Configuration saved. Changes will take effect tomorrow at midnight');
        }
      })
    );
  }

  public snooze() {
    return this.httpClient.post<void>(this.baseUri + '/snooze', {});
  }

  public getDailyUsage() {
    return this.httpClient.get<Usage>(`${this.baseUri}/usage`);
  }
}

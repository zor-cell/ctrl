import {inject, Injectable} from '@angular/core';
import {environment} from "../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {NotificationService} from "../core/services/notification.service";
import {tap} from "rxjs";
import {SessionConfig} from "./dto/SessionConfig";

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
    return this.httpClient.put<SessionConfig>(this.baseUri + '/config', updateConfigRequest).pipe(
      tap(() => {
        this.notificationService.showSuccess('Updated session configuration');
      })
    );
  }
}

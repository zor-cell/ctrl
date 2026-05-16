import {inject, Injectable} from '@angular/core';
import {environment} from "../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {NotificationService} from "../core/services/notification.service";
import {tap} from "rxjs";
import {AlgorithmConfig} from "./dto/AlgorithmConfig";

@Injectable({
  providedIn: 'root',
})
export class AlgorithmService {
  private readonly baseUri: string = environment.apiUrl + '/algorithm';
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);

  public getConfig() {
    return this.httpClient.get<AlgorithmConfig>(this.baseUri + '/config');
  }

  public updateConfig(updateConfigRequest: AlgorithmConfig) {
    return this.httpClient.put<AlgorithmConfig>(this.baseUri + '/config', updateConfigRequest).pipe(
      tap(() => {
        this.notificationService.showSuccess('Updated algorithm configuration');
      })
    );
  }
}

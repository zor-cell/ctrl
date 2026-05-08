import {inject, Injectable} from '@angular/core';
import {environment} from "../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {NotificationService} from "../core/services/notification.service";
import {tap} from "rxjs";
import {AlgorithmConfigResponse} from "./dto/AlgorithmConfigResponse";
import {AlgorithmConfigRequest} from "./dto/AlgorithmConfigRequest";

@Injectable({
  providedIn: 'root',
})
export class AlgorithmService {
  private readonly baseUri: string = environment.apiUrl + '/algorithm';
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);

  public getConfig() {
    return this.httpClient.get<AlgorithmConfigResponse>(this.baseUri + '/config');
  }

  public updateConfig(updateConfigRequest: AlgorithmConfigRequest) {
    return this.httpClient.put<AlgorithmConfigResponse>(this.baseUri + '/config', updateConfigRequest).pipe(
      tap(config => {
        this.notificationService.showSuccess('Updated algorithm configuration');
      })
    );
  }
}

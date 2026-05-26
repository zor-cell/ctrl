import { inject, Injectable, signal } from '@angular/core';
import { environment } from "../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { NotificationService } from "../core/services/notification.service";
import { tap } from "rxjs";
import { AlgorithmConfig } from "./dto/AlgorithmConfig";

@Injectable({
  providedIn: 'root',
})
export class AlgorithmService {
  private readonly baseUri: string = environment.apiUrl + '/algorithm';
  private readonly httpClient = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);

  readonly config = signal<AlgorithmConfig | null>(null);

  public getConfig() {
    return this.httpClient.get<AlgorithmConfig>(this.baseUri + '/config').pipe(
      tap(cfg => this.config.set(cfg))
    );
  }

  public updateConfig(updateConfigRequest: AlgorithmConfig) {
    return this.httpClient.put<AlgorithmConfig>(this.baseUri + '/config', updateConfigRequest).pipe(
      tap(cfg => {
        this.config.set(cfg);
        this.notificationService.showSuccess('Updated algorithm configuration');
      })
    );
  }
}

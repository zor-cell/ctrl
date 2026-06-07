import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { DatasetKey, DatasetResponse } from '../components/analytics/analytics.types';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/users/me/analytics`;

  get(key: DatasetKey): Observable<DatasetResponse> {
    return this.fetchers[key]();
  }

  private fetchers: Record<DatasetKey, () => Observable<DatasetResponse>> = {
    'overview-lastweek': () => this.http.get<DatasetResponse>(`${this.baseUri}/last-week`),
    'detailed-lastweek': () => this.http.get<DatasetResponse>(`${this.baseUri}/last-week/detailed`),
    'overview-average': () => this.http.get<DatasetResponse>(`${this.baseUri}/weekly-average`),
    'detailed-average': () => this.http.get<DatasetResponse>(`${this.baseUri}/weekly-average/detailed`),
  };
}

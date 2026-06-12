import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { DatasetKey, DatasetResponse } from './components/analytics/analytics.types';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/users/me/analytics`;

  get(key: DatasetKey, week: number): Observable<DatasetResponse> {
    switch (key) {
      case 'overview-days':
        return this.http.get<DatasetResponse>(`${this.baseUri}/week/${week}`);
      case 'overview-average':
        return this.http.get<DatasetResponse>(`${this.baseUri}/weekly-average`);
      case 'detailed-days':
        return this.http.get<DatasetResponse>(`${this.baseUri}/week/detailed/${week}`);
      case 'detailed-average':
        return this.http.get<DatasetResponse>(`${this.baseUri}/weekly-average/detailed`);
    }
  }
}

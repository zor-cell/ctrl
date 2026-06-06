import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  LastWeekResponse,
  LastWeekDetailResponse,
  AverageWeekResponse,
  AverageWeekDetailResponse,
} from '../dto/analytics.dto';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUri = `${environment.apiUrl}/users/me/analytics`;

  getLastWeek() {
    return this.http.get<LastWeekResponse>(`${this.baseUri}/last-week`);
  }

  getDayDetail() {
    return this.http.get<LastWeekDetailResponse>(`${this.baseUri}/last-week/detailed`);
  }

  getWeeklyAverage() {
    return this.http.get<AverageWeekResponse>(`${this.baseUri}/weekly-average`);
  }

  getDayOfWeekDetail() {
    return this.http.get<AverageWeekDetailResponse>(`${this.baseUri}/weekly-average/detailed`);
  }
}

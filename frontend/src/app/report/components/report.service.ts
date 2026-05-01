import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../core/services/notification.service';
import {ContentTypeEnum, Report, ReportStatus} from '../dto/report';
import { firstValueFrom, tap } from 'rxjs';
import { ReportedPostResponse } from '../dto/ReportedPostResponse';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly baseUri: string = environment.apiUrl + '/report';
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);

  public createReport(report: Report) {
    return this.httpClient.post<void>(this.baseUri, report).pipe(
      tap(() => {
        this.notificationService.showSuccess(
          'Report submitted! Thank you for helping us keep the community safe.',
        );
      }),
    );
  }

  async getReportedPosts(
    status = ReportStatus.CREATED,
    page = 0,
    postsPerPage = 20,
  ): Promise<ReportedPostResponse[]> {
    return await firstValueFrom(
      this.httpClient.get<ReportedPostResponse[]>(
        `${this.baseUri}/posts?status=${status}&page=${page}&postsPerPage=${postsPerPage}`,
      ),
    );

  }

}

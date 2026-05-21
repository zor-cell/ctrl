import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../core/services/notification.service';
import { Report, ReportAction, ReportStatus } from '../dto/report';
import { firstValueFrom, tap } from 'rxjs';
import { ReportedPostResponse } from '../dto/ReportedPostResponse';
import { ReportedSourceResponse } from '../dto/ReportedSourceResponse';
import { ReportSummaryResponse } from '../dto/ReportSummaryResponse';
import { ReportedCommentResponse } from '../dto/ReportedCommentResponse';

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

  public updateStatus(status: ReportStatus, action: ReportAction, id: string) {
    return this.httpClient.put<void>(
      `${this.baseUri}/${id}`,
      { status, action },
      { headers: { 'Content-Type': 'application/json' } },
    ).pipe(
      tap(() => {
        this.notificationService.showSuccess('Report Updated Successfully!');
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

  async getReportedSource(
    status = ReportStatus.CREATED,
    page = 0,
    postsPerPage = 20,
  ): Promise<ReportedSourceResponse[]> {
    return await firstValueFrom(
      this.httpClient.get<ReportedSourceResponse[]>(
        `${this.baseUri}/sources?status=${status}&page=${page}&postsPerPage=${postsPerPage}`,
      ),
    );
  }

  async getReportedComments(
    status = ReportStatus.CREATED,
    page = 0,
    postsPerPage = 20,
  ): Promise<ReportedCommentResponse[]> {
    return await firstValueFrom(
      this.httpClient.get<ReportedCommentResponse[]>(
        `${this.baseUri}/comments?status=${status}&page=${page}&postsPerPage=${postsPerPage}`,
      ),
    );
  }

  getReportSummary(id: string) {
    return this.httpClient.get<ReportSummaryResponse[]>(`${this.baseUri}/${id}/summary`).pipe(
        tap(() => {
          this.notificationService.showSuccess(
            'Report Updated Successfully!',
          );
        }),
      )


  }

}

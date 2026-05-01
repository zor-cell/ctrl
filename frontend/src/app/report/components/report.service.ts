import {inject, Injectable} from '@angular/core';
import {environment} from '../../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {NotificationService} from '../../core/services/notification.service';
import {Report} from '../dto/report';
import {tap} from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly baseUri: string = environment.apiUrl + "/report";
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService)

  public createReport(report: Report) {
    return this.httpClient.post<void>(this.baseUri, report).pipe(
      tap(() => {
        this.notificationService.showSuccess('Report submitted! Thank you for helping us keep the community safe.');
      })
    )
  }

}

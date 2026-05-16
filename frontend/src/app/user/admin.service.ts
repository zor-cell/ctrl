import {inject, Injectable} from '@angular/core';
import {environment} from "../../environments/environment";
import {HttpClient} from "@angular/common/http";
import {BusinessVerification, VerificationStatus} from './dto/BusinessVerification';
import {tap} from 'rxjs';
import {NotificationService} from '../core/services/notification.service';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly baseUri: string = environment.apiUrl + '/admin';
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);

  public getPendingVerifications() {
    return this.httpClient.get<BusinessVerification[]>(this.baseUri + '/verifications/pending');
  }

  public reviewRequest(status: VerificationStatus, id: string) {
    return this.httpClient.put<void>(this.baseUri +  `/verifications/${id}/decision`,
      { status },
      { headers: { 'Content-Type': 'application/json' } },
    ).pipe(
      tap(() => {
        this.notificationService.showSuccess('Report Updated Successfully!');
      }),
    );
  }
}

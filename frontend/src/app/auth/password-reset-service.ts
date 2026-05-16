import {environment} from '../../environments/environment';
import {inject, Injectable} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {NotificationService} from '../core/services/notification.service';
import {tap} from 'rxjs';
import {PasswordResetRequest} from './dto/PasswordResetRequest';
import {ForgotPasswordRequest} from './dto/ForgotPasswordRequest';


@Injectable({
  providedIn: 'root',
})
export class PasswordResetService {
  private readonly baseUri = environment.apiUrl + '/auth';
  private readonly httpClient = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);

  public resetPassword(passwordResetRequest: PasswordResetRequest) {
    return this.httpClient.post<boolean>(this.baseUri + '/reset-password', passwordResetRequest).pipe(
      tap(res => {
        if(res) {
          this.notificationService.showSuccess('Password reset successfully. You can now log in with your new password.');
        } else {
          this.notificationService.showError('Password reset failed. The reset link may be invalid or expired. Please request a new password reset email.');
        }
      }),
    )
  }

  public forgotPassword(forgotPasswordRequest: ForgotPasswordRequest) {
    return this.httpClient.post<boolean>(this.baseUri + '/forgot-password', forgotPasswordRequest).pipe(
      tap(res => {
        if(res) {
          this.notificationService.showSuccess('Password reset sent successfully if correct email was provided. Check your emails.');
        } else {
          this.notificationService.showError('Password request reset failed. Please try again later.');
        }
      }),
    )
  }
}

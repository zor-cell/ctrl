import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, tap, throwError } from 'rxjs';

import { environment } from '../../environments/environment';
import { SILENT_ERROR_CONTEXT } from '../core/interceptors';
import { UserDetails } from './dto/UserDetails';
import {UpdateProfileRequest} from "./dto/UpdateProfileRequest";
import {NotificationService} from "../core/services/notification.service";
import {UpdatePasswordRequest} from "./dto/UpdatePasswordRequest";
import {UpdateUsernameRequest} from "./dto/UpdateUsernameRequest";
import {AuthResponse} from "../auth/dto/AuthResponse";
import {AuthService} from "../auth/auth.service";
import {VerificationStatusResponse} from './dto/BusinessVerification';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly baseUri: string = environment.apiUrl + '/users';
  private httpClient = inject(HttpClient);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  private _user = signal<UserDetails | null>(null);
  public user = this._user.asReadonly();

  public loadUser() {
    return this.httpClient.get<UserDetails>(this.baseUri + '/me', {context: SILENT_ERROR_CONTEXT}).pipe(
      tap(user => {
        this._user.set(user)
      }),
      catchError(error => {
        if (error instanceof HttpErrorResponse && [401, 403].includes(error.status)) {
          this._user.set(null);
          this.authService.logout();
        }

        return throwError(() => error);
      }),
    );
  }

  public updateUser(updateRequest: UpdateProfileRequest) {
    return this.httpClient.put<UserDetails>(this.baseUri + '/me', updateRequest).pipe(
      tap(user => {
        this._user.set(user);
        this.notificationService.showSuccess('Updated profile');
      })
    );
  }

  public updateProfilePicture(file: File) {
    const formData = new FormData();
    formData.append('file', file);

    return this.httpClient.put<UserDetails>(this.baseUri + '/me/profile-picture', formData).pipe(
      tap(user => {
        this._user.set(user);
        this.notificationService.showSuccess('Updated profile picture');
      })
    );
  }

  public updatePassword(updateRequest: UpdatePasswordRequest) {
    return this.httpClient.put<void>(this.baseUri + '/me/password', updateRequest).pipe(
      tap(() => {
        this.notificationService.showSuccess('Updated password');
      })
    );
  }

  public updateUsername(request: UpdateUsernameRequest) {
    return this.httpClient.put<AuthResponse>(this.baseUri + '/me/username', request).pipe(
      tap(res => {
        this.authService.updateToken(res.token);
        this.notificationService.showSuccess('Updated username');

        this.loadUser().subscribe();
      })
    );
  }

  public verifyUser(code: string) {
    return this.httpClient.put<UserDetails>(this.baseUri + '/me/verify', {code}).pipe(
      tap(user => {
        this._user.set(user);
        this.notificationService.showSuccess('Verified Identity');
      })
    );
  }
  public requestBusinessVerification() {
    return this.httpClient.post<void>(this.baseUri + '/me/verify/business', {}).pipe(
      tap(() => {
        this.notificationService.showSuccess('Business verification request submitted');
      })
    );
  }

  public getVerificationStatus() {
    return this.httpClient.get<VerificationStatusResponse>(this.baseUri + '/me/verify', {}).pipe();

  }
}

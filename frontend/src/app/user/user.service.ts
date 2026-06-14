import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { catchError, map, tap, throwError } from 'rxjs';

import { environment } from '../../environments/environment';
import { SILENT_ERROR_CONTEXT } from '../core/interceptors';
import { UserDetails } from './dto/UserDetails';
import {UpdateProfileRequest} from "./dto/UpdateProfileRequest";
import {NotificationService} from "../core/services/notification.service";
import {UpdatePasswordRequest} from "./dto/UpdatePasswordRequest";
import {UpdateUsernameRequest} from "./dto/UpdateUsernameRequest";
import {AuthResponse} from "../auth/dto/AuthResponse";
import {AuthService} from "../auth/auth.service";
import { UserSearchResult } from './dto/Relationship';
import { PublicUserProfile } from './dto/PublicUserProfile';
import { MediaCacheService } from '../core/services/media-cache.service';
import {VerificationStatusResponse} from './dto/BusinessVerification';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly baseUri: string = environment.apiUrl + '/users';
  private httpClient = inject(HttpClient);
  private authService = inject(AuthService);
  private notificationService = inject(NotificationService);
  private mediaCacheService = inject(MediaCacheService);

  private _user = signal<UserDetails | null>(null);
  public user = this._user.asReadonly();

  public clearUser() {
    this._user.set(null);
  }

  public loadUser() {
    return this.httpClient.get<UserDetails>(this.baseUri + '/me', {context: SILENT_ERROR_CONTEXT}).pipe(
      tap(user => {
        this._user.set(user);
        this.mediaCacheService.cacheUrl(user.profilePicture, 'image', 'high');
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

  public getPublicProfile(username: string) {
    return this.httpClient.get<PublicUserProfile>(
      `${this.baseUri}/${encodeURIComponent(username)}`
    ).pipe(
      tap(profile => this.mediaCacheService.cacheUrl(profile.profilePicture, 'image', 'high'))
    );
  }

  public updateUser(updateRequest: UpdateProfileRequest, showSuccess = true) {
    return this.httpClient.put<UserDetails>(this.baseUri + '/me', updateRequest).pipe(
      tap(user => {
        this._user.set(user);
        this.mediaCacheService.cacheUrl(user.profilePicture, 'image', 'high');
        if (showSuccess) {
          this.notificationService.showSuccess('Updated profile');
        }
      })
    );
  }

  public updateProfilePicture(file: File, showSuccess = true) {
    const formData = new FormData();
    formData.append('file', file);
    const token = this.authService.token();
    const options = token
      ? { headers: new HttpHeaders({ Authorization: `Bearer ${token}` }) }
      : {};

    return this.httpClient.put<UserDetails>(
      this.baseUri + '/me/profile-picture',
      formData,
      options,
    ).pipe(
      tap(user => {
        this._user.set(user);
        this.mediaCacheService.cacheUrl(user.profilePicture, 'image', 'high');
        if (showSuccess) {
          this.notificationService.showSuccess('Updated profile picture');
        }
      })
    );
  }

  public deleteProfilePicture(showSuccess = true) {
    return this.httpClient.delete<UserDetails>(this.baseUri + '/me/profile-picture').pipe(
      tap(user => {
        this._user.set(user);
        if (showSuccess) {
          this.notificationService.showSuccess('Removed profile picture');
        }
      })
    );
  }

  public deleteAccount() {
    return this.httpClient.delete<void>(this.baseUri + '/me').pipe(
      tap(() => {
        this.clearUser();
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

  public updateUsername(request: UpdateUsernameRequest, showSuccess = true) {
    return this.httpClient.put<AuthResponse>(this.baseUri + '/me/username', request).pipe(
      tap(res => {
        this.authService.updateToken(res.token);
        if (showSuccess) {
          this.notificationService.showSuccess('Updated username');
        }

        this.loadUser().subscribe();
      })
    );
  }

  public isUsernameAvailable(username: string) {
    return this.httpClient.post<UserSearchResult[]>(
      environment.apiUrl + '/search/users',
      {
        query: username,
        excludeFriends: false,
        excludeFollowed: false,
        verifiedOnly: false,
      },
      { context: SILENT_ERROR_CONTEXT },
    ).pipe(
      map(users => !users.some(user => user.username.toLowerCase() === username.toLowerCase()))
    );
  }

  public verifyUser(code: string) {
    return this.httpClient.put<UserDetails>(this.baseUri + '/me/verify', {
      code,
      redirectUri: environment.idAustria.redirectUri,
    }).pipe(
      tap(user => {
        this._user.set(user);
        this.mediaCacheService.cacheUrl(user.profilePicture, 'image');
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

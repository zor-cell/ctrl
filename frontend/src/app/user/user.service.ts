import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { UserDetails } from './dto/UserDetails';
import {UpdateProfileRequest} from "./dto/UpdateProfileRequest";
import {NotificationService} from "../core/services/notification.service";
import {UpdatePasswordRequest} from "./dto/UpdatePasswordRequest";
import {UpdateUsernameRequest} from "./dto/UpdateUsernameRequest";
import {AuthResponse} from "../auth/dto/AuthResponse";
import {AuthService} from "../auth/auth.service";

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
    return this.httpClient.get<UserDetails>(this.baseUri + '/me').pipe(
      tap(user => this._user.set(user))
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
}
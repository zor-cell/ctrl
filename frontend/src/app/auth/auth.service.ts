import {Injectable, inject, signal} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

import {UserDetails} from '../user/dto/UserDetails'
import {RegisterRequest} from './dto/RegisterRequest'
import {LoginRequest} from './dto/LoginRequest'
import {AuthResponse} from './dto/AuthResponse'
import {Observable, tap} from "rxjs";
import {UserService} from "../user/user.service";
import {NotificationService} from "../core/services/notification.service";
import {EmailVerificationRequest} from './dto/EmailVerificationRequest';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly TOKEN_KEY = 'AUTH_TOKEN';
  private readonly baseUri: string = environment.apiUrl + '/auth';
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);

  public token = signal<string | null>(localStorage.getItem(this.TOKEN_KEY))

  public register(registerRequest: RegisterRequest) {
    return this.httpClient.post<void>(this.baseUri + '/register', registerRequest).pipe(
      tap(() => {
        this.notificationService.showSuccess('Registered user');
      })
    );
  }

  public verify_email(emailVerificationRequest: EmailVerificationRequest) : Observable<boolean> {
    return this.httpClient.post<boolean>(this.baseUri + '/verify', emailVerificationRequest).pipe(
      tap(res => {
        if(res) {
          this.notificationService.showSuccess('Email verified successfully');
        } else {
          this.notificationService.showError('Invalid verification code, try again.');
        }
      })
    );
  }

  public resendVerificationEmail(registerRequest: RegisterRequest) {
    return this.httpClient.post<void>(this.baseUri + '/email', registerRequest).pipe(
      tap(() => {
        this.notificationService.showSuccess('Verification email sent');
      })
    );
  }

  public login(loginRequest: LoginRequest) {
    return this.httpClient.post<AuthResponse>(this.baseUri + '/login', loginRequest).pipe(
      tap(res => {
        this.updateToken(res.token);
      })
    );
  }

  public logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    this.token.set(null);
  }

  public updateToken(token: string) {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.token.set(token);
  }
}

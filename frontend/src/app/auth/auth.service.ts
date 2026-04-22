import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AuthResponse } from './dto/AuthResponse';
import { LoginRequest } from './dto/LoginRequest';
import { RegisterRequest } from './dto/RegisterRequest';
import {Observable, tap} from "rxjs";
import {NotificationService} from "../core/services/notification.service";
import {EmailVerificationRequest} from './dto/EmailVerificationRequest';
import {EmailVerificationPageData} from './dto/EmailVerificationPageData';
interface JwtPayload {
  exp?: number;
}
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly TOKEN_KEY = 'AUTH_TOKEN';
  private readonly baseUri = environment.apiUrl + '/auth';
  private readonly httpClient = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);

  private readonly _token = signal<string | null>(this.getValidTokenOnStartup());
  public readonly token = this._token.asReadonly();

  public readonly isAuthenticated = computed(() => {
    const currentToken = this.token();
    if (!currentToken) {
      return false;
    }

    return !this.isTokenExpired(currentToken);
  });

  public register(registerRequest: RegisterRequest) {
    return this.httpClient.post<void>(this.baseUri + '/register', registerRequest).pipe(
      tap(() => {
        this.notificationService.showSuccess('Registered user');
      }),
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

  public resendVerificationEmail(emailVerificationData: EmailVerificationPageData) {
    return this.httpClient.post<void>(this.baseUri + '/email', emailVerificationData).pipe(
      tap(() => {
        this.notificationService.showSuccess('Verification email sent');
      })
    );
  }

  public login(loginRequest: LoginRequest) {
    return this.httpClient.post<AuthResponse>(this.baseUri + '/login', loginRequest).pipe(
      tap(res => {
        this.updateToken(res.token);
      }),
    );
  }

  public logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    this._token.set(null);
  }

  public updateToken(token: string) {
    localStorage.setItem(this.TOKEN_KEY, token);
    this._token.set(token);
  }

  public validateSession() {
    const currentToken = this._token();
    if (currentToken === null) {
      return false;
    }

    if (this.isTokenExpired(currentToken)) {
      this.logout();
      this.notificationService.showInfo('Your session has expired. Please log in again.');
      return false;
    }

    return true;
  }

  private isTokenExpired(token: string): boolean {
    try {
      const decoded = this.decodeJwtPayload(token);
      if (!decoded.exp) {
        return false;
      }

      const expirationDateInMs = decoded.exp * 1000;
      return Date.now() > expirationDateInMs - 10000;
    } catch {
      return true;
    }
  }

  private getValidTokenOnStartup(): string | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) {
      return null;
    }

    if (this.isTokenExpired(token)) {
      localStorage.removeItem(this.TOKEN_KEY);
      return null;
    }

    return token;
  }

  private decodeJwtPayload(token: string): JwtPayload {
    const [, payload] = token.split('.');

    if (!payload) {
      throw new Error('JWT payload is missing');
    }

    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
    const paddedPayload = normalizedPayload.padEnd(
      normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
      '=',
    );

    return JSON.parse(atob(paddedPayload)) as JwtPayload;
  }
}

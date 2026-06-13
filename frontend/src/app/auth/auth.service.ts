import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AuthResponse } from './dto/AuthResponse';
import { LoginRequest } from './dto/LoginRequest';
import { RegisterRequest } from './dto/RegisterRequest';
import {Observable, tap} from "rxjs";
import {NotificationService} from "../core/services/notification.service";
import { MediaCacheService } from '../core/services/media-cache.service';
import {EmailVerificationRequest} from './dto/EmailVerificationRequest';
import {EmailVerificationPageData} from './dto/EmailVerificationPageData';
import {jwtDecode} from "jwt-decode";
import {CustomJwtPayload} from "./dto/CustomJwtPayload";
import {SessionStateService} from "../session/session-state.service";

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly TOKEN_KEY = 'AUTH_TOKEN';
  private readonly baseUri = environment.apiUrl + '/auth';
  private readonly adminBaseUri = environment.apiUrl + '/admin';
  private readonly httpClient = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  private readonly mediaCacheService = inject(MediaCacheService);

  private readonly _token = signal<string | null>(this.getValidTokenOnStartup());
  public readonly token = this._token.asReadonly();

  public readonly isAuthenticated = computed(() => {
    const currentToken = this.token();
    if (!currentToken) {
      return false;
    }

    return !this.isTokenExpired(currentToken);
  });

  public readonly isAdmin = computed(() => {
    const currentToken = this.token();
    if (!currentToken || !this.isAuthenticated()) {
      return false;
    }

    try {
      const decoded = jwtDecode<CustomJwtPayload>(currentToken);
      const roles = decoded.roles || [];

      if (Array.isArray(roles)) {
        return roles.includes('ROLE_ADMIN');
      }

      return false;
    } catch {
      return false;
    }
  });

  public requestAccountDeletion(username: string) {
    return this.httpClient.delete(this.adminBaseUri + "/" + username).pipe(
      tap(() => {
        this.notificationService.showSuccess('Account successfully removed');
      }),
    )
  }

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
          this.notificationService.showError('Invalid verification, try again.');
        }
      })
    );
  }

  public resendVerificationEmail(emailVerificationData: EmailVerificationPageData) {
    return this.httpClient.post<void>(this.baseUri + '/email', emailVerificationData).pipe(
      tap(() => {
        this.notificationService.showSuccess('Verification email sent successfully if correct email was provided. Check your emails.');
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
    localStorage.removeItem(SessionStateService.LIMIT_DISMISSED_KEY);
    this._token.set(null);
    this.mediaCacheService.clear();
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
      const decoded = jwtDecode<CustomJwtPayload>(token);
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
}

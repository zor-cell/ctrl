import {Injectable, inject, signal, computed} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

import {RegisterRequest} from './dto/RegisterRequest'
import {LoginRequest} from './dto/LoginRequest'
import {AuthResponse} from './dto/AuthResponse'
import {tap} from "rxjs";
import {NotificationService} from "../core/services/notification.service";
import {jwtDecode} from "jwt-decode";

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly TOKEN_KEY = 'AUTH_TOKEN';
  private readonly baseUri: string = environment.apiUrl + '/auth';
  private httpClient = inject(HttpClient);
  private notificationService = inject(NotificationService);

  private _token = signal<string | null>(this.getValidTokenOnStartup());
  public token = this._token.asReadonly();
  public isAuthenticated = computed(() => {
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
      const decoded = jwtDecode(token);
      if (!decoded.exp) {
        return false;
      }

      const expirationDateInMs = decoded.exp * 1000;
      return Date.now() > (expirationDateInMs - 10000); //-10 seconds as buffer
    } catch (error) {
      return true;
    }
  }

  //retrieve the token on startup and checks its validity
  private getValidTokenOnStartup(): string | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) return null;

    if (this.isTokenExpired(token)) {
      localStorage.removeItem(this.TOKEN_KEY);
      return null;
    }

    return token;
  }
}

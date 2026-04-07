import {Injectable, inject, signal} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

import {UserDetails} from '../user/dto/UserDetails'
import {RegisterRequest} from './dto/RegisterRequest'
import {LoginRequest} from './dto/LoginRequest'
import {AuthResponse} from './dto/AuthResponse'
import {tap} from "rxjs";
import {UserService} from "../user/user.service";
import {NotificationService} from "../core/services/notification.service";

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

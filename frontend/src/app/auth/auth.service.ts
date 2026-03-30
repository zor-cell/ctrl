import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

import {UserDetails} from '../user/dto/UserDetails'
import {RegisterRequest} from './dto/RegisterRequest'
import {LoginRequest} from './dto/LoginRequest'
import {AuthResponse} from './dto/AuthResponse'

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly baseUri: string = environment.apiUrl + '/auth';
  private httpClient = inject(HttpClient);

  public register(registerRequest: RegisterRequest) {
    return this.httpClient.post<void>(this.baseUri + '/register', registerRequest);
  }

  public login(loginRequest: LoginRequest) {
    return this.httpClient.post<AuthResponse>(this.baseUri + '/login', loginRequest);
  }

  public logout() {
    return this.httpClient.post<void>(this.baseUri + '/logout', null);
  }
}

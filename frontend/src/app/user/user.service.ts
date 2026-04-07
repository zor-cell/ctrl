import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { UserDetails } from './dto/UserDetails';
import {UpdateProfileRequest} from "./dto/UpdateProfileRequest";

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly baseUri: string = environment.apiUrl + '/users';
  private httpClient = inject(HttpClient);

  private _user = signal<UserDetails | null>(null);
  public user = this._user.asReadonly();

  public loadUser() {
    return this.httpClient.get<UserDetails>(this.baseUri + '/me').pipe(
      tap(user => this._user.set(user))
    );
  }

  public updateUser(updateRequest: UpdateProfileRequest) {
    return this.httpClient.put<UserDetails>(this.baseUri + '/me', updateRequest).pipe(
      tap(user => this._user.set(user))
    );
  }
}
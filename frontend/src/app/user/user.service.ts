import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { UserDetails } from './dto/UserDetails';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly baseUri: string = environment.apiUrl + '/users';
  private httpClient = inject(HttpClient);

  public user = signal<UserDetails | null>(null);

  public loadUser() {
    return this.httpClient.get<UserDetails>(this.baseUri + '/me').pipe(
      tap(user => this.user.set(user))
    );
  }
}
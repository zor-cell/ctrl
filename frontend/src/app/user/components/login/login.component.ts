import { Component, inject } from '@angular/core';

import {AuthService} from '../../../auth/auth.service'

@Component({
  selector: 'app-login',
  imports: [],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private authService = inject(AuthService);

  protected login() {
    const request = {
      username: "test",
      password: "123456"
    }

    this.authService.login(request);
  }
}

import {Component, inject} from '@angular/core';

import {AuthService} from '../../auth.service'
import {LoginRequest} from "../../dto/LoginRequest";
import {FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {Button} from "primeng/button";
import {InputText} from "primeng/inputtext";
import {FloatLabel} from "primeng/floatlabel";
import {Message} from "primeng/message";
import {Router, RouterLink} from "@angular/router";

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    Button,
    InputText,
    FloatLabel,
    ReactiveFormsModule,
    Message,
    RouterLink
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private authService = inject(AuthService);
  private fb = inject(NonNullableFormBuilder);
  private router = inject(Router);

  protected loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  public login() {
    if(this.loginForm.invalid) {
      this.loginForm.markAllAsDirty();
      this.loginForm.markAllAsTouched();
      return;
    }

    const loginRequest: LoginRequest = this.loginForm.getRawValue();
    this.authService.login(loginRequest).subscribe(res => {
      this.router.navigate(['/profile']);
    });
  }
}

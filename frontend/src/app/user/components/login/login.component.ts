import {Component, inject, signal} from '@angular/core';

import {AuthService} from '../../../auth/auth.service'
import {LoginRequest} from "../../../auth/dto/LoginRequest";
import {form, FormField, required} from "@angular/forms/signals";
import {FormBuilder, FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {Password} from "primeng/password";
import {Button} from "primeng/button";
import {InputText} from "primeng/inputtext";
import {FloatLabel} from "primeng/floatlabel";
import {Message} from "primeng/message";

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    Button,
    InputText,
    FloatLabel,
    ReactiveFormsModule,
    Message
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private authService = inject(AuthService);
  private fb = inject(NonNullableFormBuilder);

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
        console.log(res);
      });
  }
}

import {Component, inject} from '@angular/core';
import {AuthService} from "../../auth.service";
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {LoginRequest} from "../../dto/LoginRequest";
import {RegisterRequest} from "../../dto/RegisterRequest";
import {Button} from "primeng/button";
import {FloatLabel} from "primeng/floatlabel";
import {InputText} from "primeng/inputtext";
import {Message} from "primeng/message";
import {Router} from "@angular/router";

@Component({
  selector: 'app-registration',
  imports: [
    Button,
    FloatLabel,
    InputText,
    Message,
    ReactiveFormsModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private fb = inject(NonNullableFormBuilder);
  private router = inject(Router);

  protected registerForm = this.fb.group({
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  public login() {
    if(this.registerForm.invalid) {
      this.registerForm.markAllAsDirty();
      this.registerForm.markAllAsTouched();
      return;
    }

    const registerRequest: RegisterRequest = this.registerForm.getRawValue();
    this.authService.register(registerRequest).subscribe(res => {
      console.log(res);
      this.router.navigate(['/login']);
    });
  }
}

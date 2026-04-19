import {Component, inject} from '@angular/core';
import {AuthService} from "../../auth.service";
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {RegisterRequest} from "../../dto/RegisterRequest";
import {Button} from "primeng/button";
import {FloatLabel} from "primeng/floatlabel";
import {InputText} from "primeng/inputtext";
import {Message} from "primeng/message";
import {Router, RouterLink} from "@angular/router";
import {DatePicker} from "primeng/datepicker";
import {environment} from '../../../../environments/environment';

@Component({
  selector: 'app-registration',
  imports: [
    Button,
    FloatLabel,
    InputText,
    Message,
    ReactiveFormsModule,
    DatePicker,
    RouterLink
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
    password: ['', Validators.required],
    name: ['', Validators.required],
    birthdate: ['', Validators.required]
  });

  public register() {
    if(this.registerForm.invalid) {
      this.registerForm.markAllAsDirty();
      this.registerForm.markAllAsTouched();
      return;
    }

    const registerRequest: RegisterRequest = this.registerForm.getRawValue();
    this.authService.register(registerRequest).subscribe(() => {
      if (environment.auth.requireEmailVerification){
        this.router.navigate(['/email_verification'], {
          state: { registerRequest }
        });
      } else {
        this.router.navigate(['/login'])
      }

    });

    this.authService.resendVerificationEmail(registerRequest).subscribe();
  }
}

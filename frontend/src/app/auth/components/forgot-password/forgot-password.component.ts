import {Component, inject} from '@angular/core';
import {FloatLabel} from 'primeng/floatlabel';
import {InputText} from 'primeng/inputtext';
import {Message} from 'primeng/message';
import {Button} from 'primeng/button'
import {NonNullableFormBuilder, ReactiveFormsModule} from "@angular/forms";
import {Router} from '@angular/router';
import {PasswordResetService} from '../../password-reset-service';
import {ForgotPasswordRequest} from '../../dto/ForgotPasswordRequest';

@Component({
  selector: 'app-forgot-password',
  imports: [Button, FloatLabel, InputText, Message, ReactiveFormsModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent {
  private passwordResetService = inject(PasswordResetService);
  private router = inject(Router);
  private fb = inject(NonNullableFormBuilder);
  submited = false;

  forgotPasswordForm = this.fb.group({
    email: [''],
  });

  submit() {
    if (this.forgotPasswordForm.invalid) {
      return;
    }

    this.submited = true;

    const email = this.forgotPasswordForm.get('email')?.value || '';
    const request: ForgotPasswordRequest = { email: email }

    this.router.navigate(['/login']);

    this.passwordResetService.forgotPassword(request).subscribe();
  }
}

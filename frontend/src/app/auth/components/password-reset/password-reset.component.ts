import {Component, inject, OnInit} from '@angular/core';
import {PasswordResetRequest} from '../../dto/PasswordResetRequest';
import {ActivatedRoute, Router} from '@angular/router';
import {NotificationService} from '../../../core/services/notification.service';
import {PasswordResetService} from '../../password-reset-service';import {Button} from 'primeng/button';


import {FloatLabel} from 'primeng/floatlabel';
import {Message} from 'primeng/message';
import {AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Password} from 'primeng/password';


@Component({
  selector: 'app-password-reset',
  imports: [Button, FloatLabel, Message, ReactiveFormsModule, Password],
  templateUrl: './password-reset.component.html',
  styleUrl: './password-reset.component.scss',
})
export class PasswordResetComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private passwordResetService = inject(PasswordResetService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);
  private fb = inject(NonNullableFormBuilder);
  passwordResetRequest: PasswordResetRequest = {
    token: '',
    password: '',
  };

  resetEmailForm = this.fb.group(
    {
      password: ['', [Validators.required]],
      confirmPassword: ['', Validators.required],
    },
    {
      validators: this.passwordMatchValidator,
    },
  );

  showPassword = false;

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (token == null) {
      this.notificationService.showError(
        'Missing password reset token. Please use the link from your email.',
      );
      return;
    }
    this.passwordResetRequest.token = token;
  }

  passwordMatchValidator(group: AbstractControl) {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  submit() {
    if (this.passwordResetRequest.token === '') {
      this.notificationService.showError(
        'Invalid password reset token. Please use the link from your email.',
      );
      return;
    }
    if (this.resetEmailForm.invalid) {
      this.resetEmailForm.markAllAsDirty();
      this.resetEmailForm.markAllAsTouched();
      return;
    }

    this.passwordResetRequest.password = this.resetEmailForm.value.password || '';

    this.passwordResetService.resetPassword(this.passwordResetRequest).subscribe((isValid) => {
      if (isValid) {
        this.router.navigate(['/login']);
        return;
      }
    });
  }
}

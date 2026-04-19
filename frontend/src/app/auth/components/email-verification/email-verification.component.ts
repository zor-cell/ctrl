import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {ChangeDetectorRef} from '@angular/core';
import {RegisterRequest} from '../../dto/RegisterRequest';
import {Button} from 'primeng/button';
import {FloatLabel} from 'primeng/floatlabel';
import {InputText} from 'primeng/inputtext';
import {Message} from 'primeng/message';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {NotificationService} from '../../../core/services/notification.service';
import {EmailVerificationRequest} from '../../dto/EmailVerificationRequest';
import {AuthService} from '../../auth.service';

@Component({
  selector: 'app-email-verification',
  imports: [Button, FloatLabel, InputText, Message, ReactiveFormsModule],
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.scss',
})
export class EmailVerificationComponent implements OnDestroy, OnInit {
  private static readonly RESEND_COOLDOWN_MS = 60_000;
  private static readonly RESEND_COOLDOWN_SECONDS = 60;

  registerRequest: RegisterRequest | null = null;
  showResendButton = false;
  resendCountdownSeconds = EmailVerificationComponent.RESEND_COOLDOWN_SECONDS;
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);
  private resendCooldownIntervalId: ReturnType<typeof setInterval> | null = null;

  verificationForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });
  codeControl = this.verificationForm.controls.code;

  constructor(private cdr: ChangeDetectorRef) {
    const navState = this.router.currentNavigation()?.extras.state?.['registerRequest'] as
      | RegisterRequest
      | undefined;
    const historyState = history.state?.registerRequest as RegisterRequest | undefined;
    this.registerRequest = navState ?? historyState ?? null;
  }

  submitCode() {
    if (this.verificationForm.invalid) {
      this.verificationForm.markAllAsTouched();
      this.verificationForm.markAllAsDirty();
      return;
    }

    const code = this.verificationForm.controls.code.value;

    if (!this.registerRequest?.username) {
      this.notificationService.showWarning('Missing registration context. Please register again.');
      return;
    }

    const verificationRequest: EmailVerificationRequest = {
      username: this.registerRequest.username,
      code: code,
    };

    this.authService.verify_email(verificationRequest).subscribe((isValid) => {
      if (isValid) {
        this.router.navigate(['/login']);
        return;
      }
    });
  }

  resendEmail() {
    this.startResendCooldown();

    if (!this.registerRequest) {
      this.notificationService.showWarning('Missing registration data. Please register again.');
      return;
    }

    this.authService.resendVerificationEmail(this.registerRequest).subscribe();
  }

  ngOnDestroy() {
    this.clearResendCooldownTimer();
  }

  ngOnInit() {
    this.startResendCooldown()
  }

  private startResendCooldown() {
    this.showResendButton = false;
    this.resendCountdownSeconds = EmailVerificationComponent.RESEND_COOLDOWN_SECONDS;

    this.clearResendCooldownTimer();

    this.resendCooldownIntervalId = setInterval(() => {
      this.resendCountdownSeconds -= 1;

      if (this.resendCountdownSeconds <= 0) {
        this.resendCountdownSeconds = 0;
        this.showResendButton = true;
        this.clearResendCooldownTimer();
      }
      this.cdr.detectChanges();
    }, EmailVerificationComponent.RESEND_COOLDOWN_MS / EmailVerificationComponent.RESEND_COOLDOWN_SECONDS);
  }

  private clearResendCooldownTimer() {
    if (this.resendCooldownIntervalId) {
      clearInterval(this.resendCooldownIntervalId);
      this.resendCooldownIntervalId = null;
    }
  }
}

import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ChangeDetectorRef} from '@angular/core';
import {Button} from 'primeng/button';
import {FloatLabel} from 'primeng/floatlabel';
import {InputText} from 'primeng/inputtext';
import {Message} from 'primeng/message';
import {NonNullableFormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {NotificationService} from '../../../core/services/notification.service';
import {EmailVerificationRequest} from '../../dto/EmailVerificationRequest';
import {AuthService} from '../../auth.service';
import {EmailVerificationPageData} from '../../dto/EmailVerificationPageData';

@Component({
  selector: 'app-email-verification',
  imports: [Button, FloatLabel, InputText, Message, ReactiveFormsModule],
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.scss',
})
export class EmailVerificationComponent implements OnDestroy, OnInit {
  private static readonly RESEND_COOLDOWN_MS = 60_000;
  private static readonly RESEND_COOLDOWN_SECONDS = 60;

  emailData: EmailVerificationPageData | null = null;
  showResendButton = false;
  resendCountdownSeconds = EmailVerificationComponent.RESEND_COOLDOWN_SECONDS;
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private notificationService = inject(NotificationService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private resendCooldownIntervalId: ReturnType<typeof setInterval> | null = null;

  verificationForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });
  codeControl = this.verificationForm.controls.code;

  submitCode() {
    if (this.verificationForm.invalid) {
      this.verificationForm.markAllAsTouched();
      this.verificationForm.markAllAsDirty();
      return;
    }

    const code = this.verificationForm.controls.code.value;

    if (!this.emailData?.username) {
      this.notificationService.showWarning('Missing registration context. Please register again.');
      return;
    }

    const verificationRequest: EmailVerificationRequest = {
      username: this.emailData.username,
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

    if (!this.emailData) {
      this.notificationService.showWarning('Missing registration data. Please register again.');
      return;
    }

    this.authService.resendVerificationEmail(this.emailData).subscribe();
  }

  ngOnDestroy() {
    this.clearResendCooldownTimer();
  }

  ngOnInit() {
    this.startResendCooldown()

    if (this.route.snapshot.paramMap.has('email') && this.route.snapshot.paramMap.has('username')) {
      this.emailData = {
        username: this.route.snapshot.paramMap.get('username') || '',
        email: this.route.snapshot.paramMap.get('email') || ''
      } as EmailVerificationPageData;
    } else {
      this.notificationService.showError('Missing registration context. Please register again.');
    }
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

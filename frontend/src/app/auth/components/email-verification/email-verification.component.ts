import {Component, inject, OnDestroy} from '@angular/core';
import {Router} from '@angular/router';
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
export class EmailVerificationComponent implements OnDestroy {
  private static readonly RESEND_COOLDOWN_MS = 60_000;

  protected registerRequest: RegisterRequest | null = null;
  protected showResendButton = false;
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private resendTimeoutId: ReturnType<typeof setTimeout> | null = null;

  protected verificationForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  constructor(
    private router: Router,
    private notificationService: NotificationService,
  ) {
    const navState = this.router.currentNavigation()?.extras.state?.['registerRequest'] as
      | RegisterRequest
      | undefined;
    const historyState = history.state?.registerRequest as RegisterRequest | undefined;
    this.registerRequest = navState ?? historyState ?? null;
    this.startResendCooldown();
  }

  protected submitCode() {
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
      code: code
    };

    this.authService.verify_email(verificationRequest).subscribe((isValid) => {
      if (isValid) {
        this.router.navigate(['/login']);
        return;
      }
    });
  }



  protected resendEmail() {
    this.startResendCooldown();

    if (!this.registerRequest) {
      this.notificationService.showWarning('Missing registration data. Please register again.');
      return;
    }

    this.authService.resendVerificationEmail(this.registerRequest).subscribe();
  }

  ngOnDestroy() {
    if (this.resendTimeoutId) {
      clearTimeout(this.resendTimeoutId);
    }
  }

  private startResendCooldown() {
    this.showResendButton = false;

    if (this.resendTimeoutId) {
      clearTimeout(this.resendTimeoutId);
    }

    this.resendTimeoutId = setTimeout(() => {
      this.showResendButton = true;
      this.resendTimeoutId = null;
    }, EmailVerificationComponent.RESEND_COOLDOWN_MS);
  }
}

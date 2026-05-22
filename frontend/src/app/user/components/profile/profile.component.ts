import {Component, computed, effect, inject, signal, OnInit} from '@angular/core';
import {FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {UserService} from "../../user.service";
import {Message} from "primeng/message";
import {DatePicker} from "primeng/datepicker";
import {FloatLabel} from "primeng/floatlabel";
import {Button} from "primeng/button";
import {InputText} from "primeng/inputtext";
import {Dialog} from "primeng/dialog";
import {passwordMatchValidator, usernameValidator} from "../../validators";
import {UpdatePasswordRequest} from "../../dto/UpdatePasswordRequest";
import {UpdateUsernameRequest} from "../../dto/UpdateUsernameRequest";
import {environment} from "../../../../environments/environment";
import {ProgressSpinner} from "primeng/progressspinner";
import {Password} from "primeng/password";
import {NgStyle} from '@angular/common';
import {VerificationStatus, VerificationStatusResponse} from '../../dto/BusinessVerification';
import {Router} from '@angular/router';
import {AuthService} from '../../../auth/auth.service';

@Component({
  selector: 'app-profile',
  imports: [
    Message,
    DatePicker,
    FloatLabel,
    ReactiveFormsModule,
    Button,
    InputText,
    Dialog,
    FormsModule,
    ProgressSpinner,
    Password,
    NgStyle,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private router = inject(Router);
  private userService = inject(UserService);
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);

  protected isLoading = signal(true);
  protected currentUser = this.userService.user;
  protected isPasswordDialogOpen = signal(false);
  protected isUsernameDialogOpen = signal(false);
  protected isVerifyDialogOpen = signal(false);
  protected verificationType = signal<'personal' | 'business'>('personal');

  protected currentVerificationStatus = signal<VerificationStatusResponse | null>(null);

  protected createdAt = computed(() => {
    const dateString = this.currentUser()?.createdAt;
    return dateString ? new Date(dateString) : null;
  });
  protected maxDate = new Date();

  protected profileForm = this.fb.group({
    name: ['', Validators.required],
    birthdate: [new Date(), Validators.required],
    email: ['', Validators.required],
    bio: [''],
  });

  protected passwordForm = this.fb.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', Validators.required],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordMatchValidator },
  );

  protected usernameForm = this.fb.group({
    newUsername: ['', [Validators.required, usernameValidator]],
  });

  constructor() {
    effect(() => {
      const currentUser = this.currentUser();

      if (currentUser) {
        this.profileForm.patchValue({
          name: currentUser.name,
          email: currentUser.email,
          bio: currentUser.bio ?? '',
          birthdate: currentUser.birthdate ? new Date(currentUser.birthdate) : new Date(),
        });

        //dynamic disabling of controls
        if (currentUser.isVerified) {
          this.profileForm.controls.name.disable();
          this.profileForm.controls.birthdate.disable();
        } else {
          this.profileForm.controls.name.enable();
          this.profileForm.controls.birthdate.enable();
        }
      }
    });
  }

  ngOnInit() {
    this.userService.loadUser().subscribe({

      next: () => {
        this.userService.getVerificationStatus().subscribe({
          next: (status) => {
            this.currentVerificationStatus.set(status)
            this.isLoading.set(false)

          },
          error: () => this.isLoading.set(false),
        });

      },
      error: () => this.isLoading.set(false),
    });


  }

  protected updateProfile() {
    if (this.profileForm.invalid) return;

    const formValues = this.profileForm.getRawValue();
    const dateObj = formValues.birthdate as Date;

    const emailChanged = this.currentUser()?.email !== formValues.email;

    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;

    this.userService.updateUser({
      name: formValues.name!,
      email: formValues.email!,
      bio: formValues.bio?.trim() ?? '',
      birthdate: formattedDate
    }).subscribe({
      next: () => {
        if (emailChanged) {
          if (environment.auth.requireEmailVerification) {
            this.authService.logout();
            this.router.navigate(['/email_verification', this.currentUser()?.username, formValues.email]);
          }
        }
      }
    }
    );
  }

  protected updatePassword() {
    if (this.passwordForm.invalid) {
      return;
    }

    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    const request: UpdatePasswordRequest = {
      currentPassword: currentPassword,
      newPassword: newPassword,
    };

    this.userService.updatePassword(request).subscribe(() => {
      this.isPasswordDialogOpen.set(false);
    });
  }

  protected updateUsername() {
    if (this.usernameForm.invalid) {
      return;
    }

    const request: UpdateUsernameRequest = this.usernameForm.getRawValue();
    this.userService.updateUsername(request).subscribe(() => {
      this.isUsernameDialogOpen.set(false);
    });
  }

  protected openPasswordDialog() {
    this.passwordForm.reset();
    this.isPasswordDialogOpen.set(true);
  }

  protected openUsernameDialog() {
    this.usernameForm.reset({
      newUsername: this.currentUser()?.username ?? '',
    });
    this.isUsernameDialogOpen.set(true);
  }

  protected openVerifyDialog() {
    this.verificationType.set('personal');
    this.isVerifyDialogOpen.set(true);
  }

  protected verifyPersonal() {
    this.isVerifyDialogOpen.set(false);
    this.initiateVerification();
  }

  protected verifyBusiness() {
    this.isVerifyDialogOpen.set(false);
    this.initiateBusinessVerification();
  }

  /**
   * Open IDAustria login page to verify user
   */
  protected initiateVerification() {
    const config = environment.idAustria;

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: config.clientId,
      redirect_uri: config.redirectUri,
      scope: config.scope,
      prompt: 'login',
    });

    window.location.href = `${config.authUrl}?${params.toString()}`;
  }

  protected getVerificationText(): string {
    const status = this.currentVerificationStatus();

    if (!status) {
      return 'Your account is unverified';
    }

    switch (status.status) {
      case VerificationStatus.NOT_VERIFIED:
        return 'Your account is unverified';
      case VerificationStatus.REJECTED:
        return 'Your verification was rejected';
      case VerificationStatus.APPROVED:
        if (status.type === 'PERSONAL') {
          return 'Verified with id austria';
        } else if (status.type === 'BUSINESS') {
          return 'Verified as business account';
        }
        return 'Account verified';
      case VerificationStatus.PENDING:
        return 'Verification pending';
      default:
        return 'your account is unverified';
    }
  }

  protected initiateBusinessVerification() {
    this.userService.requestBusinessVerification().subscribe({

        next: () => {
          this.userService.getVerificationStatus().subscribe({
            next: (status) => {
              this.currentVerificationStatus.set(status)
              this.isLoading.set(false)

            },
            error: () => this.isLoading.set(false),
          });

        },
        error: () => this.isLoading.set(false),
      }

    );
  }

  protected readonly VerificationStatus = VerificationStatus;
}

import {Component, computed, effect, inject, signal} from '@angular/core';
import {FormBuilder, FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {UserService} from "../../user.service";
import {Message} from "primeng/message";
import {DatePicker} from "primeng/datepicker";
import {FloatLabel} from "primeng/floatlabel";
import {Button} from "primeng/button";
import {InputText} from "primeng/inputtext";
import {form} from "@angular/forms/signals";
import {Dialog} from "primeng/dialog";
import {passwordMatchValidator} from "../../validators";
import {UpdatePasswordRequest} from "../../dto/UpdatePasswordRequest";
import {UpdateUsernameRequest} from "../../dto/UpdateUsernameRequest";
import {environment} from "../../../../environments/environment";

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
    FormsModule
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  private userService = inject(UserService);
  private fb = inject(NonNullableFormBuilder);

  protected currentUser = this.userService.user;
  protected isPasswordDialogOpen = signal(false);
  protected isUsernameDialogOpen = signal(false);

  protected createdAt = computed(() => {
    const dateString = this.currentUser()?.createdAt;
    return dateString ? new Date(dateString) : null;
  });
  protected maxDate = new Date();

  protected profileForm = this.fb.group({
    name: ['', Validators.required],
    birthdate: [new Date(), Validators.required],
    email: ['', Validators.required]
  });

  protected passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', Validators.required],
    confirmPassword: ['', Validators.required]
  }, {validators: passwordMatchValidator});

  protected usernameForm = this.fb.group({
    newUsername: ['', Validators.required]
  });



  constructor() {
    effect(() => {
      const currentUser = this.currentUser();

      if (currentUser) {
        this.profileForm.patchValue({
          name: currentUser.name,
          email: currentUser.email,
          birthdate: currentUser.birthdate ? new Date(currentUser.birthdate) : new Date()
        });
      }
    });
  }

  ngOnInit() {
    this.userService.loadUser().subscribe();
  }

  protected updateProfile() {
    if (this.profileForm.invalid) return;

    const formValues = this.profileForm.value;
    const dateObj = formValues.birthdate as Date;

    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;

    this.userService.updateUser({
      name: formValues.name!,
      email: formValues.email!,
      birthdate: formattedDate
    }).subscribe(res => {

    });
  }

  protected updatePassword() {
    if(this.passwordForm.invalid) {
      return;
    }

    const {currentPassword, newPassword} = this.passwordForm.getRawValue();
    const request: UpdatePasswordRequest = {
      currentPassword: currentPassword,
      newPassword: newPassword
    }

    this.userService.updatePassword(request).subscribe(res => {
      this.isPasswordDialogOpen.set(false);
    });
  }

  protected updateUsername() {
    if(this.usernameForm.invalid) {
      return;
    }

    const request: UpdateUsernameRequest = this.usernameForm.getRawValue();
    this.userService.updateUsername(request).subscribe(res => {
      this.isUsernameDialogOpen.set(false);
    })
  }

  protected openPasswordDialog() {
    this.passwordForm.reset();
    this.isPasswordDialogOpen.set(true);
  }

  protected openUsernameDialog() {
    this.usernameForm.reset({
      newUsername: this.currentUser()?.username ?? ''
    });
    this.isUsernameDialogOpen.set(true);
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
      scope: config.scope
    });

    window.location.href = `${config.authUrl}?${params.toString()}`;
  }
}

import {Component, effect, inject, signal} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
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

@Component({
  selector: 'app-profile',
  imports: [
    Message,
    DatePicker,
    FloatLabel,
    ReactiveFormsModule,
    Button,
    InputText,
    Dialog
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  private userService = inject(UserService);
  private fb = inject(FormBuilder);

  protected user = this.userService.user;
  protected isPasswordDialogOpen = signal(false);
  protected isUsernameDialogOpen = signal(false);

  protected profileForm = this.fb.group({
    name: ['', Validators.required],
    birthdate: [null as Date | null, Validators.required],
  });

  protected passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', Validators.required],
    confirmPassword: ['', Validators.required]
  }, {validators: passwordMatchValidator});

  protected usernameForm = this.fb.group({
    newUsername: ['', Validators.required]
  });

  protected maxDate = new Date();

  constructor() {
    effect(() => {
      const currentUser = this.user();

      if (currentUser) {
        this.profileForm.patchValue({
          name: currentUser.name,
          birthdate: currentUser.birthdate ? new Date(currentUser.birthdate) : null
        });
      }
    });
  }

  ngOnInit() {
    this.userService.loadUser().subscribe();
  }

  updateProfile() {
    if (this.profileForm.invalid) return;

    const formValues = this.profileForm.value;
    const dateObj = formValues.birthdate as Date;

    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;

    this.userService.updateUser({
      name: formValues.name!,
      birthdate: formattedDate
    }).subscribe(res => {

    });
  }

  updatePassword() {
    if(this.passwordForm.invalid) {
      return;
    }

    const {currentPassword, newPassword} = this.passwordForm.value;
    const request: UpdatePasswordRequest = {
      currentPassword: currentPassword ?? '',
      newPassword: newPassword ?? ''
    }

    this.userService.updatePassword(request).subscribe(res => {
      this.isPasswordDialogOpen.set(false);
    });
  }

  openPasswordDialog() {
    this.passwordForm.reset();
    this.isPasswordDialogOpen.set(true);
  }
}

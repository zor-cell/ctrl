import {AbstractControl, ValidationErrors, ValidatorFn} from "@angular/forms";

export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const newPassword = control.get('newPassword');
  const confirmPassword = control.get('confirmPassword');

  if (!newPassword || !confirmPassword) {
    return null;
  }

  if (!confirmPassword.value) {
    return null;
  }

  // check if password confirmation is correct
  return newPassword.value === confirmPassword.value ? null : { passwordsMismatch: true };
};

export const usernameValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const username = control.value as string;

  if (!username) {
    return null;
  }

  return username.length > 2 && /^[A-Za-z0-9_.]+$/.test(username)
    ? null
    : { invalidUsername: true };
};

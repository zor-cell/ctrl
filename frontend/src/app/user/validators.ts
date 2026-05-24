import {AbstractControl, ValidationErrors, ValidatorFn} from "@angular/forms";

export const USERNAME_PATTERN = /^(?=.*[A-Za-z0-9])[A-Za-z0-9_.]+$/;
export const USERNAME_VALIDATION_MESSAGE =
  'Username must be at least 3 characters, contain at least one letter or number, and may only use letters, numbers, underscores and dots.';

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

  return username.length > 2 && USERNAME_PATTERN.test(username)
    ? null
    : { invalidUsername: true };
};

import {AbstractControl, ValidationErrors, ValidatorFn} from "@angular/forms";

export function emojiValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (!value) {
      return null;
    }

    const emojiRegex = new RegExp('^[\\p{Extended_Pictographic}\\uFE0F\\u200D\\p{Emoji_Modifier}]+$', 'u');
    const isValid = emojiRegex.test(value);

    return isValid ? null : { invalidEmoji: true };
  };
}
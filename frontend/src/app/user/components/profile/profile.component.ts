import {Component, effect, inject} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {UserService} from "../../user.service";
import {Message} from "primeng/message";
import {DatePicker} from "primeng/datepicker";
import {FloatLabel} from "primeng/floatlabel";
import {Button} from "primeng/button";
import {InputText} from "primeng/inputtext";

@Component({
  selector: 'app-profile',
  imports: [
    Message,
    DatePicker,
    FloatLabel,
    ReactiveFormsModule,
    Button,
    InputText
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  private userService = inject(UserService);
  private fb = inject(FormBuilder);

  protected user = this.userService.user;

  protected profileForm = this.fb.group({
    name: ['', Validators.required],
    birthdate: [null as Date | null, Validators.required],
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
    // Trigger the HTTP call. The effect() will handle populating the form
    // once the tap() in your service updates the signal.
    this.userService.loadUser().subscribe();
  }

  onSubmit() {
    if (this.profileForm.invalid) return;

    const formValues = this.profileForm.value;
    const dateObj = formValues.birthdate as Date;

    // Format the date back to ISO-8601 string for PostgreSQL/Spring Boot
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    const day = String(dateObj.getDate()).padStart(2, '0');
    const formattedDate = `${year}-${month}-${day}`;

    this.userService.updateUser({
      name: formValues.name!,
      birthdate: formattedDate
    }).subscribe({
      next: () => {
        // You could trigger a PrimeNG Toast service here to say "Saved!"
        console.log('Profile updated successfully!');
      },
      error: (err) => {
        console.error('Failed to update profile', err);
      }
    });
  }
}

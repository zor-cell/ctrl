import {Component, inject, OnInit, signal} from '@angular/core';
import {Button} from "primeng/button";
import {FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {ProgressSpinner} from "primeng/progressspinner";
import {Tooltip} from "primeng/tooltip";
import {SessionConfigService} from "../../session-config.service";
import {SessionConfig} from "../../dto/SessionConfig";
import {ToggleSwitch} from "primeng/toggleswitch";
import {Slider} from "primeng/slider";
import {ConfirmationService} from "primeng/api";

@Component({
  selector: 'app-session-config',
  imports: [
    Button,
    FormsModule,
    ProgressSpinner,
    ProgressSpinner,
    ReactiveFormsModule,
    ToggleSwitch,
    Tooltip,
    Slider
  ],
  providers: [ConfirmationService],
  templateUrl: './config.component.html',
  styleUrl: './config.component.scss',
})
export class SessionConfigComponent implements OnInit {
  private readonly configService = inject(SessionConfigService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected isLoading = signal<boolean>(true);

  protected configForm = this.fb.group({
    timeLimitMinutes:   [50, [Validators.required, Validators.min(0)]],
    postLimit:       [50, [Validators.required, Validators.min(0)]],
    allowSnooze:   [true, [Validators.required]],
  });

  ngOnInit() {
    this.configService.getConfig().subscribe({
      next: (config: SessionConfig) => {
        this.configForm.patchValue(config);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  protected saveConfig() {
    if (this.configForm.invalid) {
      this.configForm.markAllAsTouched();
      return;
    }

    const formValue = this.configForm.getRawValue() as SessionConfig;

    this.configService.updateConfig(formValue).subscribe(updatedConfig => {
        this.configForm.patchValue(updatedConfig);
    });
  }
}

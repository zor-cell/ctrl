import {Component, inject, OnInit, signal} from '@angular/core';
import {Button} from "primeng/button";
import {FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {MultiSelect} from "primeng/multiselect";
import {ProgressSpinner} from "primeng/progressspinner";
import {Slider} from "primeng/slider";
import {Tooltip} from "primeng/tooltip";
import {SessionConfigService} from "../../session-config.service";
import {SessionConfig} from "../../dto/SessionConfig";
import {FloatLabel} from "primeng/floatlabel";
import {InputText} from "primeng/inputtext";
import {ToggleSwitch} from "primeng/toggleswitch";

@Component({
  selector: 'session-config',
  imports: [
    Button,
    FormsModule,
    ProgressSpinner,
    ProgressSpinner,
    ReactiveFormsModule,
    FloatLabel,
    InputText,
    ToggleSwitch,
    Tooltip
  ],
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
    isForcedCutoff:   [true, [Validators.required]],
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

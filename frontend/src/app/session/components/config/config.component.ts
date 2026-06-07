import {Component, inject, OnInit, signal} from '@angular/core';
import {Button} from "primeng/button";
import {FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {ProgressSpinner} from "primeng/progressspinner";
import {Tooltip} from "primeng/tooltip";
import {SessionConfigService} from "../../session-config.service";
import {SessionConfig} from "../../dto/SessionConfig";
import {ToggleSwitch} from "primeng/toggleswitch";
import {Slider} from "primeng/slider";
import {Message} from "primeng/message";
import {ConfirmationService} from "primeng/api";
import {ConfirmDialog} from "primeng/confirmdialog";

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
    Slider,
    Message,
    ConfirmDialog
  ],
  providers: [ConfirmationService],
  templateUrl: './config.component.html',
  styleUrl: './config.component.scss',
})
export class SessionConfigComponent implements OnInit {
  private readonly configService = inject(SessionConfigService);
  private readonly fb = inject(NonNullableFormBuilder);
  private confirmationService = inject(ConfirmationService);

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

  protected confirmSave() {
    if (this.configForm.invalid) {
      this.configForm.markAllAsTouched();
      return;
    }

    this.confirmationService.confirm({
      header: 'Confirm Limits',
      message: 'Your new session limits will be applied starting tomorrow. Do you want to proceed?',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Save',
      rejectLabel: 'Cancel',
      rejectButtonProps: {
        severity: 'secondary'
      },
      accept: () => {
        this.saveConfig();
      }
    })
  }

  protected saveConfig() {
    const formValue = this.configForm.getRawValue() as SessionConfig;

    this.configService.updateConfig(formValue).subscribe(updatedConfig => {
        this.configForm.patchValue(updatedConfig);
    });
  }
}

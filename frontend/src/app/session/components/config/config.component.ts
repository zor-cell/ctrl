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
import {SessionStateService} from "../../session-state.service";
import {UsageComponent} from "../usage/usage.component";
import {toSignal} from "@angular/core/rxjs-interop";
import {ConfirmDialog} from "primeng/confirmdialog";
import {TimeFormatPipe} from "../../../core/utils/time-format.pipe";

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
    UsageComponent,
    ConfirmDialog,
    TimeFormatPipe
  ],
  providers: [ConfirmationService],
  templateUrl: './config.component.html',
  styleUrl: './config.component.scss',
})
export class SessionConfigComponent implements OnInit {
  private readonly configService = inject(SessionConfigService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly sessionState = inject(SessionStateService);

  protected isLoading = signal<boolean>(true);
  protected usage = toSignal(this.configService.getDailyUsage());

  protected configForm = this.fb.group({
    timeLimitMinutes: [50, [Validators.required, Validators.min(0)]],
    postLimit: [50, [Validators.required, Validators.min(0)]],
    allowSnooze: [true, [Validators.required]],
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

    const usage = this.usage();
    if (!this.sessionState.isLimitActive() && usage && (formValue.timeLimitMinutes <= usage.elapsedMinutes || formValue.postLimit <= usage.seenPosts)) {
      this.confirmationService.confirm({
        header: 'Confirm Limit Reduction',
        message: 'Your new limits are lower than your current usage today. Saving these changes will restrict your session immediately. Do you want to proceed?',
        icon: 'pi pi-exclamation-triangle',
        acceptIcon: 'pi pi-check mr-2',
        rejectIcon: 'pi pi-times mr-2',
        rejectButtonStyleClass: 'p-button-text',
        accept: () => this.executeSave(formValue)
      });
    } else {
      this.executeSave(formValue);
    }
  }

  private executeSave(config: SessionConfig) {
    this.configService.updateConfig(config).subscribe(updatedConfig => {
        this.configForm.patchValue(updatedConfig);
      });
  }
}

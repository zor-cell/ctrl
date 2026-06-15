import {Component, computed, inject, input} from "@angular/core";
import { SessionService } from "../../../core/services/session.service";
import { ChartModule } from "primeng/chart";
import { SessionConfigService } from "../../session-config.service";
import { RouterLink } from "@angular/router";
import {toSignal} from "@angular/core/rxjs-interop";
import {ProgressSpinner} from "primeng/progressspinner";
import {Button} from "primeng/button";
import {SessionStateService} from "../../session-state.service";
import {Usage} from "../../dto/usage";

@Component({
  selector: 'app-usage',
  imports: [
    ChartModule,
    RouterLink,
    ProgressSpinner,
    Button
  ],
  templateUrl: './usage.component.html',
  styleUrl: './usage.component.scss',
})
export class UsageComponent {
  private sessionConfigService = inject(SessionConfigService);
  protected sessionState = inject(SessionStateService);
  Math = Math;

  protected sessionConfig = this.sessionConfigService.config;
  public usage = input.required<Usage | undefined>();

  protected isLoading = computed(() => this.sessionConfig() === undefined || this.usage() === undefined);

  protected triggerManualSnooze() {
    this.sessionConfigService.snooze().subscribe({
      next: () => {
        this.sessionState.resetLimit();
        this.sessionState.userDecision$.next(true);
      }
    });
  }
}


import {Component, computed, inject} from "@angular/core";
import { SessionService } from "../../../core/services/session.service";
import { ChartModule } from "primeng/chart";
import { SessionConfigService } from "../../session-config.service";
import { RouterLink } from "@angular/router";
import {toSignal} from "@angular/core/rxjs-interop";
import {ProgressSpinner} from "primeng/progressspinner";

@Component({
  selector: 'app-usage',
  imports: [
    ChartModule,
    RouterLink,
    ProgressSpinner
  ],
  templateUrl: './usage.component.html',
  styleUrl: './usage.component.scss',
})
export class UsageComponent {
  private sessionService = inject(SessionService);
  private sessionConfigService = inject(SessionConfigService);
  Math = Math;

  protected sessionConfig = toSignal(this.sessionConfigService.getConfig());
  protected usage = toSignal(this.sessionConfigService.getDailyUsage());

  protected isLoading = computed(() => this.sessionConfig() === undefined || this.usage() === undefined);
}


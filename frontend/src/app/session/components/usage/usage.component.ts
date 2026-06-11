import { Component, inject, OnInit, signal } from "@angular/core";
import { Usage, SessionService } from "../../../core/services/session.service";
import { ChartModule } from "primeng/chart";
import { SessionConfig } from "../../dto/SessionConfig";
import { SessionConfigService } from "../../session-config.service";
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-usage',
  imports: [
    ChartModule,
    RouterLink
  ],
  templateUrl: './usage.component.html',
  styleUrl: './usage.component.scss',
})
export class UsageComponent implements OnInit {
  sessionService = inject(SessionService);
  sessionConfigService = inject(SessionConfigService);
  Math = Math;

  sessionConfig = signal<SessionConfig>({
    timeLimitMinutes: 0,
    postLimit: 0,
    isForcedCutoff: false
  });

  usage = signal<Usage>({
    elapsedMinutes: 0,
    seenPosts: 0
  })

  ngOnInit() {
    this.sessionConfigService.config$.subscribe(config => {
      this.sessionConfig.set(config);
    });
    this.sessionService.sessionState$.subscribe(state => {
      this.usage.set(state);
    });
  }
}


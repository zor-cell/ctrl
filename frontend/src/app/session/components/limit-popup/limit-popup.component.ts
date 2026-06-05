import {Component, inject, signal} from '@angular/core';
import {Dialog} from "primeng/dialog";
import {Button} from "primeng/button";
import {SessionStateService} from "../../session-state.service";
import {SessionService} from "../../../core/services/session.service";
import {SessionConfigService} from "../../session-config.service";
import {SessionConfig} from "../../dto/SessionConfig";

@Component({
  selector: 'app-limit-popup',
  imports: [
    Dialog,
    Button
  ],
  templateUrl: './limit-popup.component.html',
  styleUrl: './limit-popup.component.scss',
})
export class LimitPopupComponent {
  private sessionService = inject(SessionConfigService);
  protected sessionState = inject(SessionStateService);

  protected sessionConfig = signal<SessionConfig | null>(null);

  ngOnInit() {
    this.sessionService.getConfig().subscribe(res => {
      this.sessionConfig.set(res);
    });
  }
}

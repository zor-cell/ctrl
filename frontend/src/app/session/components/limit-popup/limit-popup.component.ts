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
  public isConfirmingSoftCutoff = signal<boolean>(false);

  public promptSoftCutoffConfirm() {
    this.isConfirmingSoftCutoff.set(true);
  }

  public cancelSoftCutoffConfirm() {
    this.isConfirmingSoftCutoff.set(false);
  }

  public executeSoftCutoff() {
    this.isConfirmingSoftCutoff.set(false);
    this.softCutoff();
  }

  ngOnInit() {
    this.sessionService.getConfig().subscribe(res => {
      this.sessionConfig.set(res);
    });
  }

  protected hardCutoff() {
    this.sessionState.closeModal()
  }

  protected softCutoff() {
    this.sessionService.resetSoftCutoff().subscribe(res => {
      this.sessionState.closeModal();
    })
  }
}

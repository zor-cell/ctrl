import {Component, inject} from '@angular/core';
import {Dialog} from "primeng/dialog";
import {Button} from "primeng/button";
import {SessionStateService} from "../../session-state.service";
import {SessionConfigService} from "../../session-config.service";
import {toObservable, toSignal} from "@angular/core/rxjs-interop";
import {filter, startWith, switchMap} from "rxjs";

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

  private limitReached$ = toObservable(this.sessionState.isLimitReached);

  protected sessionConfig = toSignal(
    this.limitReached$.pipe(
      startWith(true),
      filter(isReached => isReached === true),
      switchMap(() => this.sessionService.getConfig())
    ),
    { initialValue: null }
  );

  protected hardCutoff() {
    this.sessionState.closeModal()
  }

  protected softCutoff() {
    this.sessionService.resetSoftCutoff().subscribe(() => {
      this.sessionState.closeModal();
    })
  }
}

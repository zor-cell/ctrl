import {Component, inject, signal} from '@angular/core';
import {Dialog} from "primeng/dialog";
import {Button} from "primeng/button";
import {SessionStateService} from "../../session-state.service";
import {SessionConfigService} from "../../session-config.service";
import {toObservable, toSignal} from "@angular/core/rxjs-interop";
import {filter, startWith, switchMap} from "rxjs";
import {PrimeTemplate} from "primeng/api";
import {Checkbox} from "primeng/checkbox";
import {FormsModule} from "@angular/forms";
import {NgClass} from "@angular/common";

@Component({
  selector: 'app-limit-popup',
  imports: [
    Dialog,
    Button,
    PrimeTemplate,
    Checkbox,
    FormsModule,
    NgClass
  ],
  templateUrl: './limit-popup.component.html',
  styleUrl: './limit-popup.component.scss',
})
export class LimitPopupComponent {
  private sessionService = inject(SessionConfigService);
  protected sessionState = inject(SessionStateService);

  private limitReached$ = toObservable(this.sessionState.showModal);

  protected sessionConfig = toSignal(
    this.limitReached$.pipe(
      startWith(false),
      filter(isReached => isReached === true),
      switchMap(() => this.sessionService.getConfig())
    ),
    { initialValue: null }
  );
  protected dontShowAgain = signal<boolean>(true);

  protected hardCutoff() {
    this.sessionState.closeModal(false, this.dontShowAgain());
  }

  protected softCutoff() {
    this.sessionService.snooze().subscribe(() => {
      this.sessionState.closeModal(true);
    })
  }
}

import { Injectable, signal } from "@angular/core";
import {Subject} from "rxjs";

@Injectable({
  providedIn: 'root',
})
export class SessionStateService {
  public static LIMIT_DISMISSED_KEY = 'session_limit_dismissed';

  public isLimitActive = signal<boolean>(false);
  public showModal = signal<boolean>(false);
  public limitMessage = signal<string>('');

  public userDecision$ = new Subject<boolean>();

  public resetLimit() {
    localStorage.removeItem(SessionStateService.LIMIT_DISMISSED_KEY);
    this.isLimitActive.set(false);
  }

  public triggerLimitModal(message: string) {
    this.isLimitActive.set(true);

    const dontShowModal = localStorage.getItem(SessionStateService.LIMIT_DISMISSED_KEY) === 'true';
    if (dontShowModal) {
      setTimeout(() => this.userDecision$.next(false));
      return;
    }

    this.limitMessage.set(message);
    this.showModal.set(true);
  }

  public closeModal(withSnooze: boolean, dontShowAgain = false) {
    this.showModal.set(false);

    if (withSnooze) {
      this.resetLimit();
    } else {
      if (dontShowAgain) {
        localStorage.setItem(SessionStateService.LIMIT_DISMISSED_KEY, 'true');
      } else {
        localStorage.removeItem(SessionStateService.LIMIT_DISMISSED_KEY);
      }
    }

    this.userDecision$.next(withSnooze);
  }
}

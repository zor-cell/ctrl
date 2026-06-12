import { Injectable, signal } from "@angular/core";

@Injectable({
  providedIn: 'root',
})
export class SessionStateService {
  public static LIMIT_DISMISSED_KEY = 'session_limit_dismissed';

  public isLimitActive = signal<boolean>(false);
  public showModal = signal<boolean>(false);
  public limitMessage = signal<string>('');

  public resetLimit() {
    localStorage.removeItem(SessionStateService.LIMIT_DISMISSED_KEY);
    this.isLimitActive.set(false);
  }

  public triggerLimitModal(message: string) {
    const dontShowModal = localStorage.getItem(SessionStateService.LIMIT_DISMISSED_KEY) === 'true';
    if (dontShowModal) {
      return;
    }

    this.limitMessage.set(message);
    this.isLimitActive.set(true);
    this.showModal.set(true);
  }

  public closeModal(withSnooze: boolean) {
    this.showModal.set(false);

    if (withSnooze) {
      this.resetLimit();
    } else {
      localStorage.setItem(SessionStateService.LIMIT_DISMISSED_KEY, 'true');
    }
  }
}

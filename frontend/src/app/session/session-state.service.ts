import {Injectable, signal} from "@angular/core";

@Injectable({
  providedIn: 'root',
})
export class SessionStateService {
  public isLimitActive = signal<boolean>(false);
  public showModal = signal<boolean>(false);
  public limitMessage = signal<string>('');

  public resetLimit() {
    this.isLimitActive.set(false);
  }

  public triggerLimitModal(message: string) {
    this.limitMessage.set(message);
    this.isLimitActive.set(true);
    this.showModal.set(true);
  }

  public closeModal() {
    this.showModal.set(false);
  }
}

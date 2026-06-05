import {Injectable, signal} from "@angular/core";

@Injectable({
  providedIn: 'root',
})
export class SessionStateService {
  public isLimitReached = signal<boolean>(false);
  public limitMessage = signal<string>('');

  public triggerLimitModal(message: string) {
    this.limitMessage.set(message);
    this.isLimitReached.set(true);
  }

  public closeModal() {
    this.isLimitReached.set(false);
  }
}

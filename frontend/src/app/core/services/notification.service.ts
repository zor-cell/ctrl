import {inject, Injectable} from '@angular/core';
import {MessageService} from "primeng/api";

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private messageService = inject(MessageService);

  showSuccess(message: string, title = 'Success') {
    this.messageService.add({ severity: 'success', summary: title, detail: message });
  }

  showError(error: unknown, title = 'Error') {
    const e = error instanceof Object ? (error as {error: unknown}).error : error;
    const status: number = (error as {status: number}).status;

    let message = e instanceof ProgressEvent ? (error as {message: string}).message : e;
    if (status === 0) {
      message = 'Cannot connect to the server. Please check your internet connection or try again later'
    } else if ((error as {error: unknown}).error && typeof (error as {error: unknown}).error === 'string') {
      message = (error as {error: string}).error;
    } else if((error as {error: unknown}).error && Array.isArray((error as {error: unknown}).error)) {
      message = (error as {error: unknown[]}).error;
    } else if ((error as {error: {message: string}}).error?.message) {
      message = (error as {error: {message: string}}).error.message;
    } else if((error as {error: {detail: unknown}}).error?.detail) {
      message = (error as {error: {detail: unknown}}).error.detail;
    } else if ((error as {message: string}).message) {
      message = (error as {message: string}).message;
    }

    this.messageService.add({ severity: 'error', summary: `${title} ${status}`, detail: message as string });
  }

  showInfo(message: string, title = 'Information') {
    this.messageService.add({ severity: 'info', summary: title, detail: message });
  }

  showWarning(message: string, title = 'Warning') {
    this.messageService.add({ severity: 'warn', summary: title, detail: message });
  }
}

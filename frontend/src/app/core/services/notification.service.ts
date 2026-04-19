import {inject, Injectable} from '@angular/core';
import {MessageService} from "primeng/api";

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private messageService = inject(MessageService);

  showSuccess(message: string, title: string = 'Success') {
    this.messageService.add({ severity: 'success', summary: title, detail: message });
  }

  showError(error: any, title: string = 'Error') {
    const e = error instanceof Object ? error.error : error;
    const status: number = error.status;

    let message = e instanceof ProgressEvent ? error.message : e;
    if (status === 0) {
      // backend unreachable
      message = 'Cannot connect to the server. Please check your internet connection or try again later'
    } else if (error.error && typeof error.error === 'string') {
      // text error
      message = error.error;
    } else if(error.error && Array.isArray(error.error)) {
      //array error
      message = error.error.map((err: any) => `• ${err}`).join('\n');
    } else if (error.error && error.error.message) {
      // JSON error
      message = error.error.message;
    } else if(error.error && error.error.detail) {
      message = error.error.detail;
    } else if (error.message) {
      message = error.message;
    }

    this.messageService.add({ severity: 'error', summary: `${title} ${status}`, detail: message });
  }

  showInfo(message: string, title: string = 'Information') {
    this.messageService.add({ severity: 'info', summary: title, detail: message });
  }

  showWarning(message: string, title: string = 'Warning') {
    this.messageService.add({ severity: 'warn', summary: title, detail: message });
  }
}

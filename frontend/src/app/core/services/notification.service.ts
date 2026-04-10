import {inject, Injectable} from '@angular/core';
import {MessageService} from "primeng/api";
import {HttpErrorResponse} from "@angular/common/http";

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private messageService = inject(MessageService);

  showSuccess(message: string, title = 'Success') {
    this.messageService.add({ severity: 'success', summary: title, detail: message });
  }

  showError(error: unknown, title = 'Error') {
    const httpError = error as {
      status?: number;
      message?: string;
      error?: unknown;
    };

    const e = error instanceof HttpErrorResponse ? error.error :
      (error instanceof Error ? error.message : String(error));
    const status = httpError.status ?? 0;

    let message = e instanceof ProgressEvent ? httpError.message : e;
    if (status === 0) {
      message = 'Cannot connect to the server. Please check your internet connection or try again later'
    } else if (typeof httpError.error === 'string') {
      // text error
      message = httpError.error;
    } else if(Array.isArray(httpError.error)) {
      //array error
      message = httpError.error;
    } else if (isRecordWithKey(httpError.error, 'message')) {
      // JSON error
      message = httpError.error.message;
    } else if (isRecordWithKey(httpError.error, 'detail')) {
      message = httpError.error.detail;
    } else if (httpError.message) {
      message = httpError.message;
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

function isRecordWithKey<T extends string>(
  value: unknown,
  key: T
): value is Record<T, string> {
  return typeof value === 'object' && value !== null && key in value;
}

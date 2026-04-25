import {inject, Injectable} from '@angular/core';
import {MessageService} from "primeng/api";

interface ApiError {
  error?: unknown;
  status?: number;
  message?: string;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private messageService = inject(MessageService);

  showSuccess(message: string, title = 'Success') {
    this.messageService.add({ severity: 'success', summary: title, detail: message });
  }

  showError(error: ApiError | string | ProgressEvent, title = 'Error') {
    const apiError = typeof error === 'object' && error !== null ? error as ApiError : {};
    const e = 'error' in apiError ? apiError.error : error;
    const status = apiError.status;

    let message = e instanceof ProgressEvent ? apiError.message : e;
    if (status === 0) {
      // backend unreachable
      message = 'Cannot connect to the server. Please check your internet connection or try again later'
    } else if (typeof apiError.error === 'string') {
      // text error
      message = apiError.error;
    } else if(Array.isArray(apiError.error)) {
      //array error
      message = apiError.error.map(err => `• ${err}`).join('\n');
    } else if (isErrorWithMessage(apiError.error)) {
      // JSON error
      message = apiError.error.message;
    } else if(isErrorWithDetail(apiError.error)) {
      message = apiError.error.detail;
    } else if (apiError.message) {
      message = apiError.message;
    }

    this.messageService.add({ severity: 'error', summary: `${title} ${status}`, detail: message });
  }

  showInfo(message: string, title = 'Information') {
    this.messageService.add({ severity: 'info', summary: title, detail: message });
  }

  showWarning(message: string, title = 'Warning') {
    this.messageService.add({ severity: 'warn', summary: title, detail: message });
  }
}

function isErrorWithMessage(error: unknown): error is { message: string } {
  return typeof error === 'object'
    && error !== null
    && 'message' in error
    && typeof error.message === 'string';
}

function isErrorWithDetail(error: unknown): error is { detail: string } {
  return typeof error === 'object'
    && error !== null
    && 'detail' in error
    && typeof error.detail === 'string';
}

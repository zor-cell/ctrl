import {HttpContext, HttpContextToken, HttpInterceptorFn} from "@angular/common/http";
import {inject} from "@angular/core";
import {catchError, throwError} from "rxjs";
import {NotificationService} from "./services/notification.service";
import {AuthService} from "../auth/auth.service";
import {SessionStateService} from "../session/session-state.service";

/**
 * Intercepts all HTTP requests and appends the JWT authentication if user is authenticated
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.token();
  const publicEndpoints = ['/api/auth/'];

  if (!token || !authService.validateSession()) {
    return next(req);
  }

  if (publicEndpoints.some(endpoint => req.url.includes(endpoint))) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};


const SILENT_ERROR_HANDLER = new HttpContextToken<boolean>(() => false);

/**
 * A context variable that can be appended as a request context to not show the error banner on error
 */
export const SILENT_ERROR_CONTEXT = new HttpContext().set(SILENT_ERROR_HANDLER, true);


/**
 * Intercepts all HTTP responses and shows an error banner if the response was an error
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notification = inject(NotificationService);
  const sessionState = inject(SessionStateService);

  const silentErrorHandling = req.context.get(SILENT_ERROR_HANDLER);

  return next(req).pipe(
    catchError(err => {
      //handle session limits
      if(err.status === 429 && err.error?.errorCode === 'SESSION_LIMIT_EXCEEDED') {
        sessionState.triggerLimitModal(err.error.message);
        return throwError(() => err);
      }

      if(!silentErrorHandling) {
        notification.showError(err);
      }

      return throwError(() => err);
    })
  )
};

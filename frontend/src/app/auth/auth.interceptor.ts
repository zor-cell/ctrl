import {HttpInterceptorFn} from "@angular/common/http";
import {inject} from "@angular/core";
import {AuthService} from "./auth.service";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.token();

  //dont add token to auth endpoints
  if (!token || req.url.includes('/api/auth/')) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};
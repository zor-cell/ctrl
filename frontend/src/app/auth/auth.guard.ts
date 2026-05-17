import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { UserService } from '../user/user.service';
import { AuthService } from './auth.service';
import {NotificationService} from "../core/services/notification.service";

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const userService = inject(UserService);
  const router = inject(Router);

  if (!authService.validateSession()) {
    return router.createUrlTree(['/login']);
  }

  if (userService.user()) {
    return true;
  }

  return userService.loadUser().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notificationService = inject(NotificationService);

  if (authService.isAdmin()) {
    return true;
  }

  notificationService.showError('You do not have permission to access this page.');

  return router.createUrlTree(['/feed']);
}

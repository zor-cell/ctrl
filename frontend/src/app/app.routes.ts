import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import {LoginComponent} from "./auth/components/login/login.component";
import {RegisterComponent} from "./auth/components/registration/register.component";
import {EmailVerificationComponent} from './auth/components/email-verification/email-verification.component';
import {PasswordResetComponent} from './auth/components/password-reset/password-reset.component';
import {ForgotPasswordComponent} from './auth/components/forgot-password/forgot-password.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'reset-password', component: PasswordResetComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  {
    path: 'feed',
    loadComponent: () =>
      import('./feed/components/feed/feed.component').then(m => m.FeedComponent),
  },
  {
    path: 'report',
    loadComponent: () =>
      import('./report/components/report-review/report-review.component').then(m => m.ReportReviewComponent),
  },
  {
    path: 'post',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./post/post-page.component').then(m => m.PostPageComponent),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./post/post-page.component').then(m => m.PostPageComponent),
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./user/components/profile/profile.component').then(
        m => m.ProfileComponent,
      ),
  },
  {
    path: 'search',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./user/components/search-page/search-page.component').then(
        m => m.SearchPageComponent,
      ),
  },
  { path: 'email_verification/:username/:email', component: EmailVerificationComponent },
  {
    path: 'followed',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./user/components/followed-page/followed-page.component').then(
        m => m.FollowedPageComponent,
      ),
  },
  {
    path: 'verification-callback',
    loadComponent: () =>
      import('./user/components/verification-callback/verification-callback.component').then(
        m => m.VerificationCallbackComponent,
      ),
  },
];

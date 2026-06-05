import { Routes } from '@angular/router';
import {adminGuard, authGuard} from './auth/auth.guard';
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
    canActivate: [authGuard],
    loadComponent: () =>
      import('./feed/components/feed/feed.component').then(m => m.FeedComponent),
  },
  {
    path: 'report',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./report/components/report-review/report-review.component').then(m => m.ReportReviewComponent),
  },
  {
    path: 'business-verification',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./user/components/business-verification/business-verification.component').then(m => m.BusinessVerificationComponent),
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
    path: 'profile/:username',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./post/post-page.component').then(m => m.PostPageComponent),
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./settings/components/settings/settings.component').then(
        m => m.SettingsComponent,
      ),
  },
  {
    path: 'search',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./search/search.routes').then(m => m.searchRoutes),
  },
  {
    path: 'posts/:postId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./post/components/post-detail-page/post-detail-page.component').then(m => m.PostDetailPageComponent),
  },
  {
    path: 'chat',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./chat/chat-page.component').then(
        m => m.ChatPageComponent,
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
  {
    path: 'algorithm',
    loadComponent: () =>
      import('./algorithm/components/config/config.component').then(
        m => m.AlgorithmConfigComponent,
      ),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./user/components/admin/admin-panel/admin-panel.component').then(
        m => m.AdminPanelComponent
      ),
  },
];
